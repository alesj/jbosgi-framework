/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.osgi.framework.metadata.internal;

import java.io.Serializable;
import java.util.StringTokenizer;

import org.jboss.classloading.spi.version.VersionComparator;
import org.jboss.osgi.framework.metadata.VersionRange;
import org.osgi.framework.Version;

/**
 * Represents an OSGi version range:
 * version-range ::= interval | atleast
 * interval ::= ( '[' | '(' ) floor ',' ceiling ( ']' | ')' )
 * atleast ::= version
 * floor ::= version
 * ceiling ::= version
 *
 * [TODO] do we really need this extra class or just use our version range?
 * @author Scott.Stark@jboss.org
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 * @author adrian@jboss.org
 */
public class AbstractVersionRange extends org.jboss.classloading.spi.version.VersionRange implements VersionRange, Serializable
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -178825219621278882L;
   
   /**
    * Get the version range from a string
    * 
    * @param rangeSpec the range spec
    * @return the version range
    */
   public static VersionRange valueOf(String rangeSpec)
   {
      return parseRangeSpec(rangeSpec);
   }

   /**
    * Parse a range spec
    * 
    * @param rangeSpec
    * @return the version range
    */
   public static VersionRange parseRangeSpec(String rangeSpec)
   {
      if (rangeSpec == null)
         throw new IllegalArgumentException("Null rangeSpec");
      
      // Handle version strings with quotes 
      if (rangeSpec.startsWith("\"") && rangeSpec.endsWith("\""))
         rangeSpec = rangeSpec.substring(1, rangeSpec.length() - 1);
      
      Version floor = null;
      Version ceiling = null;
      StringTokenizer st = new StringTokenizer(rangeSpec, ",[]()", true);
      Boolean floorIsGreaterThan = null;
      Boolean ceilingIsLessThan = null;
      boolean mid = false;
      while (st.hasMoreTokens())
      {
         String token = st.nextToken();
         if (token.equals("["))
            floorIsGreaterThan = false;
         else if (token.equals("("))
            floorIsGreaterThan = true;
         else if (token.equals("]"))
            ceilingIsLessThan = false;
         else if (token.equals(")"))
            ceilingIsLessThan = true;
         else if (token.equals(","))
            mid = true;
         else if (token.equals("\"") == false)
         {
            // A version token
            if (floor == null)
               floor = new Version(token);
            else
               ceiling = new Version(token);
         }

      }
      // check for parenthesis
      if (floorIsGreaterThan == null || ceilingIsLessThan == null)
      {
         // non-empty interval usage
         if (mid)
            throw new IllegalArgumentException("Missing parenthesis: " + rangeSpec);
         // single value
         floorIsGreaterThan = false;
         ceilingIsLessThan = false;
      }

      return new AbstractVersionRange(floor, ceiling, floorIsGreaterThan, ceilingIsLessThan);
   }

   /**
    * Create a new AbstractVersionRange.
    * 
    * @param floor the floor
    * @param ceiling the ceiling
    * @param floorIsLessThan whether the floor is <
    * @param ceilingIsLessThan whether the ceiling is <
    */
   public AbstractVersionRange(Version floor, Version ceiling, boolean floorIsLessThan, boolean ceilingIsLessThan)
   {
      super(floor == null ? Version.emptyVersion : floor, floorIsLessThan == false, ceiling, ceilingIsLessThan == false);
   }

   /**
    * Get the floor
    * 
    * @return the floor
    */
   public Version getFloor()
   {
      return (Version) getLow();
   }

   /**
    * Get the ceiling
    * 
    * @return the ceiling
    */
   public Version getCeiling()
   {
      return (Version) getHigh();
   }

   /**
    * Test whether the version is in range
    * 
    * @return true when the version is in range
    */
   public boolean isInRange(Version v)
   {
      return super.isInRange(v);
   }

   /**
    * OSGiVersionToOSGiVersionComparator.
    */
   public static class OSGiVersionToOSGiVersionComparator implements VersionComparator<Version, Version>
   {
      public int compare(Version t, Version u)
      {
         return t.compareTo(u);
      }
   }

   /**
    * OSGiVersionToVersionComparator.
    */
   public static class OSGiVersionToVersionComparator implements VersionComparator<Version, org.jboss.classloading.spi.version.Version>
   {
      public int compare(Version t, org.jboss.classloading.spi.version.Version u)
      {
         int result = t.getMajor() - u.getMajor();
         if (result != 0)
            return result;

         result = t.getMinor() - u.getMinor();
         if (result != 0)
            return result;

         result = t.getMicro() - u.getMicro();
         if (result != 0)
            return result;

         String q1 = t.getQualifier();
         String q2 = u.getQualifier();
         return q1.compareTo(q2);
      }
   }

   /**
    * VersionToStringComparator.
    */
   public static class OSGiVersionToStringComparator implements VersionComparator<Version, String>
   {
      public int compare(Version t, String u)
      {
         return t.compareTo(Version.parseVersion(u));
      }
   }
}
