/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.osgi.nativecode.bundleA;

//$Id: NativeCodeActivatorA.java 99304 2010-01-12 17:29:06Z thomas.diesler@jboss.com $

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import java.util.HashMap;
import java.util.Map;

public class NativeCodeActivatorA implements BundleActivator
{
   private static Map<String, String> osAliases = new HashMap<String, String>();

   static
   {
      osAliases.put("SymbianOS", "Epoc32");
      osAliases.put("hp-ux", "HPUX");
      osAliases.put("Linux", "Linux");
      osAliases.put("Mac OS", "MacOS");
      osAliases.put("Mac OS X", "MacOSX");
      osAliases.put("OS/2", "OS2");
      osAliases.put("procnto", "QNX");
      osAliases.put("Win95", "Windows95");
      osAliases.put("Windows 95", "Windows95");
      osAliases.put("Win32", "Windows95");
      osAliases.put("Win98", "Windows98");
      osAliases.put("Windows 98", "Windows98");
      osAliases.put("Win32", "Windows98");
      osAliases.put("WinNT", "WindowsNT");
      osAliases.put("Windows NT", "WindowsNT");
      osAliases.put("Win32", "WindowsNT");
      osAliases.put("WinCE", "WindowsCE");
      osAliases.put("Windows CE", "WindowsCE");
      osAliases.put("Win2000", "Windows2000");
      osAliases.put("Windows 2000", "Windows2000");
      osAliases.put("Win32", "Windows2000");
      osAliases.put("Win2003", "Windows2003");
      osAliases.put("Windows 2003", "Windows2003");
      osAliases.put("Win32", "Windows2003");
      osAliases.put("Windows Server 2003", "Windows2003");
      osAliases.put("WinXP", "WindowsXP");
      osAliases.put("Windows XP", "WindowsXP");
      osAliases.put("Win32", "WindowsXP");
      osAliases.put("WinVista", "WindowsVista");
      osAliases.put("Windows Vista", "WindowsVista");
      osAliases.put("Win32", "WindowsVista");
      osAliases.put("Windows 7", "Windows7");
   }

   public void start(BundleContext context) throws BundleException
   {
      Bundle bundle = context.getBundle();
      try
      {
         System.loadLibrary("Native");
         throw new IllegalStateException("UnsatisfiedLinkError expected");
      }
      catch (UnsatisfiedLinkError ex)
      {
         String exmsg = ex.getMessage();
         long bundleid = bundle.getBundleId();
         String os = System.getProperty("os.name");
         String osAlias = osAliases.get(os);
         String suffix = osAlias != null ? osAlias.toLowerCase() : "";
         if ("".equals(suffix))
            System.err.println("No such OS mapped to alias: " + os);

         String substr = "osgi-store/bundle-" + bundleid + "/" + suffix;
         if (exmsg.indexOf(substr) < 0)
            throw new UnsatisfiedLinkError("Cannot find '" + substr + "' in '" + exmsg + "'");
      }
   }

   public void stop(BundleContext context)
   {
   }
}