/**
 *  Copyright (c) 2003-2005 Fernando Dobladez
 *
 *  This file is part of AntDoclet.
 *
 *  AntDoclet is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  AntDoclet is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with AntDoclet; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.neuroning.antdoclet.latex;

import java.awt.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;
import java.util.*;
import java.io.*;

/**
 * This class implements a simple HTML to LaTeX translator.
 * It's very ugly to my taste... but it works good enough for now.
 * It should be replaced with new code (may be limit it to xhtml, and use any
 * XML parser to implement it).
 * 
 * It's a modified version of source code I took from Soren Caspersen, who in
 * turn took it from Gregg Wonderly (http://texdoclet.dev.java.net/)
 * 
 * It's implemented using the HTML parser that is part of Swing.
 * 
 * Fernando Dobladez <dobladez@gmail.com>
 * 
 */
public class HTML2Latex extends HTMLEditorKit.ParserCallback {

    /**
     * Buffer containing the translated HTML.
     */
    StringBuffer ret;
    Stack tblstk = new Stack();
    TableInfo tblinfo;
    int verbat = 0;
    int colIdx = 0;
    Hashtable colors = new Hashtable(10);
    String block = "";
    String refurl = null;
    String doNotPrintURL = null;
    String refname = null;
    String refimg = null;
    boolean notex = false;
    int imageindex = 0;
    boolean _hyperref = true;
    boolean escape = true;

    /**
     * Constructs a new instance.
     * 
     * @param StringBuffer
     *                    The <CODE>StringBuffer</CODE> where the translated HTML is
     *                    appended.
     */
    public HTML2Latex(StringBuffer ret) {
        this.ret = ret;
    }

    public HTML2Latex() {
    }

    /**
     * This method handles simple HTML tags (eg. <CODE>&lt;HR&gt;</CODE>-tags).
     * It is called by the parser whenever such a tag is encountered.
     */
    public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attrSet,
            int pos) {
        if (tag.toString().equalsIgnoreCase("tex")) {
            if (attrSet.containsAttribute(HTML.Attribute.ENDTAG, "true")) {
                notex = false;
            } else {
                String tex = (String) attrSet.getAttribute("txt");
                ret.append(tex);
                notex = true;
            }
        } else if (notex) {
            return;
        } else if (tag == HTML.Tag.META) {
        } else if (tag == HTML.Tag.HR) {
//            String sz = (String) attrSet.getAttribute(HTML.Attribute.SIZE);
//            int size = 1;
//            if (sz != null) size = Integer.parseInt(sz);
//            ret.append("\\mbox{}\\newline\\rule[2mm]{\\hsize}{"+(1*size*.5)+"mm}\\newline\n");

            // FERD. Using hsize is wrong, since the rule may not start on
            // the very left, in which case \hsize would span over
            // the right margin.
            ret.append("\\hspace*{3cm}\\hrulefill\\hspace*{3cm}\\newline\n\n"); // FERD
        } else if (tag == HTML.Tag.BR) {
            ret.append("\\mbox{}\\newline ");
        }
    }

    /**
     * This method handles HTML tags that mark a beginning (eg. <CODE>&lt;P&gt;</CODE>-tags).
     * It is called by the parser whenever such a tag is encountered.
     */
    public void handleStartTag(HTML.Tag tag, MutableAttributeSet attrSet,
            int pos) {
        if (notex) return;

        if (tag == HTML.Tag.PRE) {
            // ret.append( "{\\tt\\small\n\\mbox{}\\newline ");
            // verbat++;
            ret.append("\n\\begin{lstlisting}\n");
            escape = false;
        } else if (tag == HTML.Tag.H1) {
            ret.append("\\chapter*{");
        } else if (tag == HTML.Tag.H2) {
            ret.append("\\section*{");
        } else if (tag == HTML.Tag.H3) {
            ret.append("\\subsection*{");
        } else if (tag == HTML.Tag.H4) {
            ret.append("\\subsubsection*{");
        } else if (tag == HTML.Tag.H5) {
            ret.append("\\subsubsection*{");
        } else if (tag == HTML.Tag.H6) {
            ret.append("\\subsubsection*{");
        } else if (tag == HTML.Tag.SUB) {
            ret.append("$_{");
        } else if (tag == HTML.Tag.SUP) {
            ret.append("$^{");
            // } else if (tag == HTML.Tag.HTML) {
        } else if (tag == HTML.Tag.HEAD) {
        } else if (tag == HTML.Tag.CENTER) {
            ret.append("\\makebox[\\hsize]{ ");
        } else if (tag == HTML.Tag.TITLE) {
            ret.append("\\chapter{");
        } else if (tag == HTML.Tag.FORM) {
        } else if (tag == HTML.Tag.INPUT) {
        } else if (tag == HTML.Tag.BODY) {
        } else if (tag == HTML.Tag.CODE) {
            ret.append( "{\\tt\\small " );
            // ret.append("\\api{"); // ferd
        } else if (tag == HTML.Tag.TT) {
            ret.append("{\\tt ");
        } else if (tag == HTML.Tag.P) {
            ret.append("\n\n");
        } else if (tag == HTML.Tag.B) {
            ret.append("{\\bf ");
        } else if (tag == HTML.Tag.STRONG) {
            ret.append("{\\bf ");
        } else if (tag == HTML.Tag.A) {
            refurl = (String) attrSet.getAttribute(HTML.Attribute.HREF);
            doNotPrintURL = (String) attrSet.getAttribute("donotprinturl");
            if (refurl != null) {
                if (_hyperref) {
                    /*
                     * if (refurl.toLowerCase().startsWith("doc-files")) { File
                     * file = new File(TexDoclet.packageDir, refurl); if
                     * (file.exists()) { if
                     * (TexDoclet.appendencies.contains(file.getPath())) {
                     * refurl = (String)
                     * TexDoclet.appendencies.get(file.getPath()); } else {
                     * refurl = "appendix" + new
                     * Integer(TexDoclet.appendencies.size()+1);
                     * TexDoclet.appendencies.put(file.getPath(), refurl); }
                     * ret.append("\\hyperref{}{" + refurl + "}{}{"); return; } }
                     */
                    String sharp = "";
                    if (refurl.indexOf("#") >= 0) {
                        sharp = refurl.substring(refurl.indexOf("#") + 1,
                                                 refurl.length());
                        if (sharp.indexOf("%") >= 0) sharp = ""; // Don't
                                                                                        // know
                                                                                        // what
                                                                                        // to
                                                                                        // do
                                                                                        // with
                                                                                        // '%'
                        refurl = refurl.substring(0, refurl.indexOf("#"));
                    }
                    ret.append("\\hyperref{" + refurl + "}{" + sharp + "}{}{");
                    // ret.append("\\href{" + refurl + "}{");
                } else
                    ret.append("{\\bf ");
            } else {
                refname = (String) attrSet.getAttribute(HTML.Attribute.NAME);
                if (refname != null && _hyperref) {
                    ret.append("\\hyperdef{" + refname + "}{");
                }
            }

        } else if (tag == HTML.Tag.OL) {
            ret.append("\n\\begin{enumerate}");
        } else if (tag == HTML.Tag.DL) {
            ret.append("\n\\begin{itemize}");
        } else if (tag == HTML.Tag.LI) {
            ret.append("\n\\item{\\vskip -.8ex ");
        } else if (tag == HTML.Tag.DT) {
            ret.append("\\item[");
        } else if (tag == HTML.Tag.DD) {
            ret.append("{");
        } else if (tag == HTML.Tag.UL) {
            ret.append("\\begin{itemize}");
        } else if (tag == HTML.Tag.I) {
            ret.append("{\\it ");
        } else if (tag == HTML.Tag.TABLE) {
            tblstk.push(tblinfo);
            tblinfo = new TableInfo();
            ret = tblinfo.startTable(ret, attrSet);
        } else if (tag == HTML.Tag.TH) {
            tblinfo.startHeadCol(attrSet);
        } else if (tag == HTML.Tag.TD) {
            tblinfo.startCol(attrSet);
        } else if (tag == HTML.Tag.TR) {
            tblinfo.startRow(attrSet);
        } else if (tag == HTML.Tag.FONT) {
            //String sz = (String) attrSet.getAttribute(HTML.Attribute.SIZE);
            String col = (String) attrSet.getAttribute(HTML.Attribute.COLOR);
            ret.append("{");
            if (col != null) {
                if ("redgreenbluewhiteyellowblackcyanmagenta".indexOf(col) != -1)
                    ret.append("\\color{" + col + "}");
                else {
                    if ("abcdefABCDEF0123456789".indexOf(col.charAt(0)) != -1) {
                        Color cc = new Color((int) Long.parseLong(col, 16));
                        String name = (String) colors
                                .get("color" + cc.getRGB());
                        if (name == null) {
                            ret.append("\\definecolor{color" + colIdx
                                       + "}[rgb]{" + (cc.getRed() / 255.0)
                                       + "," + (cc.getBlue() / 255.0) + ","
                                       + (cc.getGreen() / 255.0) + "}");
                            name = "color" + colIdx;
                            colIdx++;
                            colors.put("color" + cc.getRGB(), name);
                        }
                        ret.append("\\color{" + name + "}");
                        ++colIdx;
                    }
                }
            }
        }

    }

    /**
     * This method handles HTML tags that mark an ending (eg. <CODE>&lt;/P&gt;</CODE>-tags).
     * It is called by the parser whenever such a tag is encountered.
     */
    public void handleEndTag(HTML.Tag tag, int pos) {

        if (notex) {
            return;
        } else if (tag == HTML.Tag.PRE) {
            // verbat--;
            // ret.append( "}\n" );
            ret.append("\n\\end{lstlisting}\n");
            escape = true;

        } else if (tag == HTML.Tag.H1) {
            ret.append("}");
        } else if (tag == HTML.Tag.H2) {
            ret.append("}");
        } else if (tag == HTML.Tag.H3) {
            ret.append("}");
        } else if (tag == HTML.Tag.H4) {
            ret.append("}");
        } else if (tag == HTML.Tag.H5) {
            ret.append("}");
        } else if (tag == HTML.Tag.H6) {
            ret.append("}");
        } else if (tag == HTML.Tag.SUB) {
            ret.append("}$");
        } else if (tag == HTML.Tag.SUP) {
            ret.append("}$");
            // } else if (tag == HTML.Tag.HTML) {
        } else if (tag == HTML.Tag.HEAD) {
        } else if (tag == HTML.Tag.CENTER) {
            ret.append("}");
        } else if (tag == HTML.Tag.TITLE) {
            ret.append("}{");
        } else if (tag == HTML.Tag.FORM) {
        } else if (tag == HTML.Tag.INPUT) {
        } else if (tag == HTML.Tag.BODY) {
        } else if (tag == HTML.Tag.CODE) {
            ret.append("}");
        } else if (tag == HTML.Tag.TT) {
            ret.append("}");
        } else if (tag == HTML.Tag.P) {
            ret.append("\n\n");
        } else if (tag == HTML.Tag.B) {
            ret.append("}");
        } else if (tag == HTML.Tag.STRONG) {
            ret.append("}");
        } else if (tag == HTML.Tag.A) {
            if (refurl != null) {
                ret.append("} ");
                if (doNotPrintURL == null) {
                    if (!refurl.equals("")) {
                        ret.append("(at ");
                        ret.append(fixText(refurl));
                        ret.append(")");
                    }
                }
            } else if (refname != null) {
                ret.append("}");
            }

        } else if (tag == HTML.Tag.LI) {
            ret.append("}");
        } else if (tag == HTML.Tag.DT) {
            ret.append("]");
        } else if (tag == HTML.Tag.DD) {
            ret.append("}");
        } else if (tag == HTML.Tag.DL) {// /
            ret.append("\n\\end{itemize}\n");
        } else if (tag == HTML.Tag.OL) {
            ret.append("\n\\end{enumerate}\n");
        } else if (tag == HTML.Tag.UL) {
            ret.append("\n\\end{itemize}\n");
        } else if (tag == HTML.Tag.I) {
            ret.append("}");
        } else if (tag == HTML.Tag.TABLE) {
            ret = tblinfo.endTable();
            tblinfo = (TableInfo) tblstk.pop();
        } else if (tag == HTML.Tag.TH) {
            tblinfo.endCol();
        } else if (tag == HTML.Tag.TD) {
            tblinfo.endCol();
        } else if (tag == HTML.Tag.TR) {
            tblinfo.endRow();
        } else if (tag == HTML.Tag.FONT) {
            ret.append("}");
        }

    }

    /**
     * This method handles all other text.
     */
    public void handleText(char[] data, int pos) {
        String str = new String(data);
        for (int i = 0; i < str.length(); ++i) {
            int c = str.charAt(i);
            if (notex) continue;

            if (!escape) {
                ret.append((char) c);
                continue;
            }

            switch (c) {
            case 160: // &nbsp;
                ret.append("\\phantom{ }");
                break;
            case ' ':
                if (verbat > 0) {
                    ret.append("\\phantom{ }");
                } else {
                    ret.append(' ');
                }
                break;
            case '[':
                if (i < str.length() - 1 && str.charAt(i + 1) == ' ') {
                    ret.append("\\lbrack\\ ");
                    i++;
                } else {
                    ret.append("\\lbrack ");
                }
                break;
            case ']':
                if (i < str.length() - 1 && str.charAt(i + 1) == ' ') {
                    ret.append("\\rbrack\\ ");
                    i++;
                } else {
                    ret.append("\\rbrack ");
                }
                break;
            case '_':
            case '%':
            case '$':
            case '#':
            case '}':
            case '{':
            case '&':
                ret.append('\\');
                ret.append((char) c);
                if (i < str.length() - 1 && str.charAt(i + 1) == ' ') {
                    ret.append("\\ ");
                    i++;
                }
                break;
            case 'æ':
                if (i < str.length() - 1 && str.charAt(i + 1) == ' ') {
                    ret.append("\\ae\\ ");
                    i++;
                } else {
                    ret.append("\\ae ");
                }
                break;
            case 'Æ':
                if (i < str.length() - 1 && str.charAt(i + 1) == ' ') {
                    ret.append("\\AE\\ ");
                    i++;
                } else {
                    ret.append("\\AE ");
                }
                break;
            case 'å':
                if (i < str.length() - 1 && str.charAt(i + 1) == ' ') {
                    ret.append("\\aa\\ ");
                    i++;
                } else {
                    ret.append("\\aa ");
                }
                break;
            case 'Å':
                if (i < str.length() - 1 && str.charAt(i + 1) == ' ') {
                    ret.append("\\AA\\ ");
                    i++;
                } else {
                    ret.append("\\AA ");
                }
                break;
            case 'ø':
                if (i < str.length() - 1 && str.charAt(i + 1) == ' ') {
                    ret.append("\\o\\ ");
                    i++;
                } else {
                    ret.append("\\o ");
                }
                break;
            case 'Ø':
                if (i < str.length() - 1 && str.charAt(i + 1) == ' ') {
                    ret.append("\\O\\ ");
                    i++;
                } else {
                    ret.append("\\O ");
                }
                break;
            case '^':
                ret.append("$\\wedge$");
                break;
            case '<':
                ret.append("\\textless ");
                break;
            case '\r':
            case '\n':
                if (tblstk.size() > 0) {
                    // Swallow new lines while tables are in progress,
                    // <tr> controls new line emission.
                    if (verbat > 0) {
                        ret.append("}\\mbox{}\\newline\n{\\tt\\small ");
                    } else
                        ret.append(" ");
                } else {
                    if (verbat > 0)
                        ret.append("}\\mbox{}\\newline\n{\\tt\\small ");
                    else if ((i + 1) < str.length() && str.charAt(i + 1) == 10) {
                        ret.append("\\bl ");
                        ++i;
                    } else
                        ret.append((char) c);
                }
                break;
            case '/':
                ret.append("/");
                break;
            case '>':
                ret.append("\\textgreater ");
                break;
            case '\\':
                ret.append("\\textbackslash ");
                break;
            default:
                ret.append((char) c);
                break;
            }
        }
    }

    /**
     * Converts a HTML string into <TEX txt="\LaTeX{}">LaTeX</TEX> using an
     * instance of <CODE>HTML2Latex</CODE>.
     * 
     * @returns The converted string.
     */
    public static String fixText(String str) {
        StringBuffer result = new StringBuffer(str.length());
        HTML2Latex b = new HTML2Latex(result);
        Reader reader = new StringReader(str);
        try {
            new ParserDelegator().parse(reader, b, false);
        } catch (IOException e) {
        }
        return new String(result);
    }

}
