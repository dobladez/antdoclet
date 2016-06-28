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
package com.neuroning.antdoclet;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.TaskContainer;
import org.apache.tools.ant.types.EnumeratedAttribute;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * An object of this class represents a Java class that is: an Ant Task, or an
 * Ant Type.
 * 
 * It provides information about the Task/Type's attributes, nested elements and
 * more.
 * 
 * It's intended to be used for documenting Ant Tasks/Types
 * 
 * @author Fernando Dobladez <dobladez@gmail.com>
 */
public class AntDoc implements Comparable {

    /**
     * An IntrospectionHelper (from Ant) to interpret ant-specific conventions
     * from Tasks and Types
     */
    private IntrospectionHelper introHelper;

    /**
     * Javadoc description for this type.
     */
    private ClassDoc doc;

    /**
     * Javadoc "root node"
     */
    private RootDoc rootdoc;

    /**
     * The java Class for this type
     */
    private Class clazz;

    private AntDoc(IntrospectionHelper ih, RootDoc rootdoc, ClassDoc doc,
            Class clazz) {
        this.doc = doc;
        this.rootdoc = rootdoc;
        this.introHelper = ih;
        this.clazz = clazz;
    }

    public static AntDoc getInstance(String clazz) {
        return getInstance(clazz, null);
    }

    public static AntDoc getInstance(Class clazz) {
        return getInstance(clazz, null);
    }

    public static AntDoc getInstance(String clazz, RootDoc rootdoc) {
        Class c = null;

        try {
            c = Class.forName(clazz);
        } catch (Throwable ee) {
            // try inner class (replacing last . for $)
            int lastdot = clazz.lastIndexOf(".");

            if (lastdot >= 0) {
                String newName = clazz.substring(0, lastdot) + "$"
                                 + clazz.substring(lastdot + 1);

                // System.out.println("trying inner:"+newName);

                try {
                    c = Class.forName(newName);
                } catch (Throwable e) {
                    System.err.println("WARNING: AntDoclet couldn't find '"
                                       + clazz
                                       + "'. Make sure it's in the CLASSPATH");
                }
            }
        }

        return c != null ? getInstance(c, rootdoc) : null;
    }

    public static AntDoc getInstance(Class clazz, RootDoc rootdoc) {
        AntDoc d = null;

        IntrospectionHelper ih = IntrospectionHelper.getHelper(clazz);

        ClassDoc doc = null;

        if (rootdoc != null) doc = rootdoc.classNamed(clazz.getName());

        // Filter out those types/tasks that are marked as "ignored"
        if (!"true".equalsIgnoreCase(Util.tagAttributeValue(doc, "ant.task",
                                                            "ignore"))
            && !"true".equalsIgnoreCase(Util.tagAttributeValue(doc, "ant.type",
                                                               "ignore"))) {
            d = new AntDoc(ih, rootdoc, doc, clazz);
        }

        return d;
    }

    /**
     * @return Whether this represents an Ant Task (otherwise, it is assumed as
     *               a Type)
     */
    public boolean isTask() {
        return Task.class.isAssignableFrom(this.clazz);
    }

    /**
     * @return Is this an Ant Task Container?
     */
    public boolean isTaskContainer() {
        return TaskContainer.class.isAssignableFrom(this.clazz);
    }

       /**
        * @return Should this task/type be excluded?
        */
    public boolean isIgnored() {
        return  "true".equalsIgnoreCase(Util.tagAttributeValue(doc, "ant.task", "ignore"))
                  || "true".equalsIgnoreCase(Util.tagAttributeValue(doc, "ant.type", "ignore"));
    }

    /**
     * @return Is the source code for this type included in this javadoc run?
     */
    public boolean sourceIncluded() {
        return doc != null ? doc.isIncluded() : false;
    }

    /**
     * 
     * @return The source comment (description) for this class (task/type)
     */
    public String getComment() {
        return doc != null ? doc.commentText() : null;
    }

    /**
     * @return Short comment for this class (basically, the first sentence)
     */
    public String getShortComment() {
        if (doc == null) return null;

        Tag[] firstTags = doc.firstSentenceTags();

        if (firstTags.length > 0 && firstTags[0] != null)
			return firstTags[0].text();

        return null;

    }

    /**
     * Get the attributes in this class from Ant's point of view.
     * 
     * @return Collection of Ant attributes, excluding those inherited from
     * org.apache.tools.ant.Task, or null if there are none
     */
    public Collection getAttributes() {
        ArrayList attrs = Collections.list(introHelper.getAttributes());

        if (attrs.isEmpty()) return null;
        else {
          // filter out all attributes inherited from Task, since they are
          // common to all Ant Tasks and tend to confuse
          try {
            BeanInfo beanInfo = Introspector.getBeanInfo(Task.class);
            PropertyDescriptor[] commonProps = beanInfo
                    .getPropertyDescriptors();
            for (int i = 0; i < commonProps.length; i++) {

                String propName = commonProps[i].getName().toLowerCase();
                // System.out.println("Ignoring task property:"+propName);
                attrs.remove(propName);
            }

          } catch (Exception e) {
            e.printStackTrace();
          }

          return attrs;
        }
    }

    /**
     * 
     * @return a collection of the "Nested Elements" that this Ant tasks accepts, or null if there are none
     */
    public Iterator<String> getNestedElements() {
      Enumeration elements = introHelper.getNestedElements();
      if (elements.hasMoreElements())
      {
      Collection c = new HashSet<String>();
      while (elements.hasMoreElements()) {
        c.add(elements.nextElement());
      }
      return c.iterator();
      }
      
      else {
        return null;
      }
    }

    /**
     * Get the extension points for this class. 
     * Derived from the add(instance) or addConfigured(instance) methods.
     * Each class is technically an Ant type, but typically you wont want to document it; just its concrete implementations.
     * You can pass these strings to getImplementingClasses() to finds the available implementations.
     * @return The fully qualified class names, or null if there are none
     */
    public Iterator<String> getNestedTypes() {
      List<Method> mm = introHelper.getExtensionPoints();
      if (mm.isEmpty()) {
        return null;
      }
      else {
        Collection c = new HashSet<String>();
        for(Method m:mm)
        {
          String classname = m.getParameterTypes()[0].getName();
          c.add(classname);
        }
        return c.iterator();
      }
    }
    
    /** 
     * Find all subclasses of the given abstract class or interface.
     * Does NOT match the class itself. 
     */
    public Iterator<String> getImplementingClasses(String className)
    {
      List<String> imps = new ArrayList<String>();
      ClassDoc thisClass = rootdoc.classNamed(className);
      for (ClassDoc cd : rootdoc.classes()) {
        if (cd.subclassOf(thisClass) && !cd.qualifiedName().equals(className)) {
          imps.add(cd.qualifiedName());
        }
      }
      return imps.iterator();
    }
    
    /**
     * Get the AntDoc for the specified (arbitrary) class.
     * 
     * @param className
     * @return null if the class cannot be found on the classpath.
     */
    public AntDoc getTypeDoc(String className)
    {
      return getInstance(className, rootdoc);
    }

    /**
     * Get the comment for the add or addconfigured method for the specified class (extension).
     * 
     * @return The source comment (description), or null if the class cannot be found.
     */
    public String getCommentForType(String type) {
      notNull(type, "type");
      MethodDoc m = getMethodForType(doc, type);
      return m==null ? null : m.commentText();
    }

    public String getFullClassName() {
        return clazz.getName();
    }

    /**
     * 
     * @return true if this refers to an inner-class
     */
    public boolean isInnerClass() {
        if (doc == null) return false;

        boolean inner = (doc.containingClass() != null);

        return inner;

    }

    /**
     * Get the comment about the requirement of this attribute. The comment if
     * extracted from the
     * 
     * @ant.required tag
     * @param attribute
     * @return A comment. A null String if this attribute is not declared as
     *               required
     */
    public String getAttributeRequired(String attribute) {
        MethodDoc method = getMethodFor(this.doc, attribute);
        if (method == null) {
        	return null;
        }
        return Util.tagValue(method, "ant.required");
    }

    /**
     * Get the comment about the "non-requirement" of this attribute. The
     * comment if extracted from the
     * 
     * @ant.not-required tag
     * @param attribute
     * @return A comment. A null String if this attribute is not declared as
     *               non-required
     */
    public String getAttributeNotRequired(String attribute) {
        MethodDoc method = getMethodFor(this.doc, attribute);
        if (method == null) {
        	return null;
        }
        return Util.tagValue(method, "ant.not-required");
    }

    /**
     * Return the "category" of this Ant "task" or "type"
     * 
     * @returns The value of the "category" attribute of the
     * @ant.task or
     * @ant.type if it exists.
     * 
     */
    public String getAntCategory() {

        String antCategory = Util.tagAttributeValue(this.doc, "ant.task",
                                                    "category");

        if (antCategory == null)
			antCategory = Util.tagAttributeValue(this.doc, "ant.type",
                                                 "category");

        if (antCategory == null && getContainerDoc() != null)
			antCategory = getContainerDoc().getAntCategory();

        return antCategory;

    }

    /**
     * @returns true if the class has a
     * @ant.type or
     * @ant.task tag in it
     */
    public boolean isTagged() {
        return Util.tagAttributeValue(this.doc, "ant.task", "name") != null
               || Util.tagAttributeValue(this.doc, "ant.type", "name") != null;
    }

    /**
     * Return the name of this type from Ant's perspective
     * 
     * @returns The value of the
     * @ant.task or
     * @ant.type if it exists. Otherwise, the Java class name.
     * 
     */
    public String getAntName() {
        String antName = Util.tagAttributeValue(this.doc, "ant.task", "name");
        
        if (antName == null)
			antName = Util.tagAttributeValue(this.doc, "ant.type", "name");

        // Handle inner class case
        if (antName == null && getContainerDoc() != null) {

            antName = getContainerDoc().getAntName()
                      + "."
                      + this.clazz
                              .getName()
                              .substring(
                                         this.clazz.getName().lastIndexOf('$') + 1);
        }


        if (antName == null) antName = typeToString(this.clazz);

        
        return antName;
    }

    /**
     * 
     * @see #getNestedElements()
     * @param elementName
     * @return The java type for the specified element accepted by this task
     */
    public Class getElementType(String elementName) {
        return introHelper.getElementType(elementName);
    }

    /**
     * Return a new AntDoc for the given "element"
     */
    public AntDoc getElementDoc(String elementName) {

        return getInstance(getElementType(elementName), this.rootdoc);
    }

    /**
     * Return a new AntDoc for the "container" of this type. Only makes sense
     * for inner classes.
     * 
     */
    public AntDoc getContainerDoc() {
        if (!isInnerClass()) return null;

        return getInstance(this.doc.containingClass().qualifiedName(),
                           this.rootdoc);
    }

    /**
     * Return the name of the type for the specified attribute
     */
    public String getAttributeType(String attributeName) {
        return typeToString(introHelper.getAttributeType(attributeName));
    }

    /**
     * Retrieves the method comment for the given attribute.
     * The comment of the setter is used preferably to the getter comment.
     * 
     * @param attribute
     * @return The comment for the specified attribute
     */
    public String getAttributeComment(String attribute) {
        MethodDoc method = getMethodFor(this.doc, attribute);
        if (method == null) {
        	return new String();
        }
        return method.commentText();
    }
    
    /**
     * Searches the given class for the appropriate setter or getter for the given attribute.
     * This method only returns the getter if no setter is available.
     * If the given class provides no method declaration, the superclasses are
     * searched recursively.
     * 
     * @param attribute
     * @param methods
     * @return The MethodDoc for the given attribute or null if not found
     */
    private static MethodDoc getMethodFor(ClassDoc classDoc, String attribute) {
    	if (classDoc == null) {
    		return null;
    	}
    	MethodDoc result = null;
    	MethodDoc[] methods = classDoc.methods();
        for (int i = 0; i < methods.length; i++) {

            // we give priority to the documentation on the "setter" method of
            // the attribute
            // but if the documentation is only on the "getter", use it
            // we give priority to the documentation on the "setter" method of
            // the attribute
            // but if the documentation is only on the "getter", use it
            if (methods[i].name().equalsIgnoreCase("set" + attribute)) {
				return methods[i];
			} else if (methods[i].name().equalsIgnoreCase("get" + attribute)) {
				result = methods[i];
			}
        }
        if (result == null) {
        	return getMethodFor(classDoc.superclass(), attribute);
        }
        return result;
    }

    /**
     * Searches the given class for the appropriate setter or getter for the given attribute.
     * This method only returns the getter if no setter is available.
     * If the given class provides no method declaration, the superclasses are
     * searched recursively.
     * 
     * @param attribute
     * @param methods
     * @return The MethodDoc for the given attribute or null if not found
     */
    private static MethodDoc getMethodForType(ClassDoc classDoc, String nestedType) {
      notNull(classDoc, "classDoc");
      notNull(nestedType, "nestedType");

      MethodDoc result = null;
      MethodDoc[] methods = classDoc.methods();
      for (MethodDoc method : methods) {
        if (method.name().equalsIgnoreCase("add")||method.name().equalsIgnoreCase("addConfigured")) {
          com.sun.javadoc.Parameter[] params = method.parameters();
          if (params.length == 1) {
            // Ugly. I have the method, why can't Javadoc give me the comment directly?
            if (nestedType.endsWith(params[0].type().typeName())) { 
              result = method;
              break;
            }
          }
        }
      }
      if (result == null && classDoc.superclass() != null) {
        return getMethodForType(classDoc.superclass(), nestedType);
      }
      return result;
    }

    /**
     * characters&lt;/echo&gt;
     * 
     * @return true if this Ant Task/Type expects characters in the element
     *               body.
     */
    public boolean supportsCharacters() {
        return introHelper.supportsCharacters();
    }

    // Private helper methods:

    /**
     * Create a "displayable" name for the given type
     * 
     * @param clazz
     * @return a string with the name for the given type
     */
    private static String typeToString(Class clazz) {
        String fullName = clazz.getName();

        String name = fullName.lastIndexOf(".") >= 0 ? fullName
                .substring(fullName.lastIndexOf(".") + 1) : fullName;

        String result = name.replace('$', '.'); // inner's
                                                                    // use
                                                                    // dollar
                                                                    // signs

        if (EnumeratedAttribute.class.isAssignableFrom(clazz)) {
            try {
                EnumeratedAttribute att = (EnumeratedAttribute) clazz
                        .newInstance();
                result = "String [";

                String[] values = att.getValues();
                result += "\"" + values[0] + "\"";
                for (int i = 1; i < values.length; i++)
					result += ", \"" + values[i] + "\"";

                result += "]";
            } catch (java.lang.IllegalAccessException iae) {
                // ignore, may a protected/private Enumeration
            }
            catch(Exception e) {
                e.printStackTrace();
            }

        }

        return result;

    }

    public int compareTo(Object o) {
        AntDoc otherDoc = (AntDoc)o;
        
        String fullName1 = getAntCategory() +":" + getAntName();
        String fullName2 = otherDoc.getAntCategory() +":"+ otherDoc.getAntName();
        
        return fullName1.compareTo(fullName2);
    }

    
    /**
     * Argument check for methods - not nullable.
     * 
     * Typed, so you can use instancevar = notNull(arg,"arg");
     * 
     * @param <T>
     * @param t
     * @param msg Message for 
     * @throws NullPointerException if t is null
     * @return 
     */
    public static <T> T notNull(T t, String msg) {
        if (t == null) {
            throw new NullPointerException(msg);
        }
        return t;
    }
}
