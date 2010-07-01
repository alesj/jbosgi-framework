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
package org.jboss.test.osgi.bundle.metadata;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import org.jboss.osgi.framework.metadata.ActivationPolicyMetaData;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.PackageAttribute;
import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.jboss.osgi.framework.metadata.VersionRange;
import org.jboss.osgi.framework.metadata.internal.AbstractPackageAttribute;
import org.jboss.osgi.framework.metadata.internal.AbstractParameter;
import org.jboss.osgi.framework.metadata.internal.AbstractParameterizedAttribute;
import org.jboss.osgi.framework.metadata.internal.OSGiManifestMetaData;
import org.jboss.osgi.framework.metadata.internal.OSGiParameters;
import org.junit.Test;

/**
 * Test OSGi header values.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class HeaderValuesTestCase extends AbstractManifestTestCase
{
   public HeaderValuesTestCase(String name)
   {
      super(name);
   }

   @Test
   public void testSerializable() throws Exception
   {
      Manifest manifest = getManifest(createName(null));
      OSGiMetaData metaData = new OSGiManifestMetaData(manifest);
      ActivationPolicyMetaData apmd = metaData.getBundleActivationPolicy();
      assertNotNull(apmd);

      metaData = serializeDeserialize((Serializable)metaData, OSGiMetaData.class);
      String bundleName = metaData.getBundleName();
      assertEquals("SomeBundleName", bundleName);
      apmd = metaData.getBundleActivationPolicy();
      List<String> includes = apmd.getIncludes();
      assertEquals(includes, Arrays.asList("org.jboss"));
      List<String> excludes = apmd.getExcludes();
      assertEquals(excludes, Arrays.asList("com.acme"));
   }

   @Test
   public void testSimpleManifest() throws Exception
   {
      Manifest manifest = getManifest(createName("Simple"));
      OSGiMetaData metaData = new OSGiManifestMetaData(manifest);

      assertEquals("org.jboss.test.osgi.bundle.metadata.BundleActivator", metaData.getBundleActivator());
      assertEquals(Arrays.asList("test.jar", "mc.jar", "seam.jar"), metaData.getBundleClassPath());
      assertEquals("TestHeadersManifest", metaData.getBundleDescription());
      assertEquals("OSGI-INF/l10n/bundle", metaData.getBundleLocalization());
      assertEquals(2, metaData.getBundleManifestVersion());
      assertEquals("TestBundle", metaData.getBundleName());
      assertEquals("UniqueName", metaData.getBundleSymbolicName());
      assertEquals(new URL("file://test.jar"), metaData.getBundleUpdateLocation());
      assertEquals("1.2.3.GA", metaData.getBundleVersion());
      assertEquals(Arrays.asList("ena", "dva", "tri"), metaData.getRequiredExecutionEnvironment());
   }

   @Test
   public void testJavaccManifest() throws Exception
   {
      Manifest manifest = getManifest(createName("JavaCC"));
      OSGiMetaData metaData = new OSGiManifestMetaData(manifest);

      List<ParameterizedAttribute> bundleNativeCode = new ArrayList<ParameterizedAttribute>();
      Map<String, Parameter> bnc1 = new HashMap<String, Parameter>();
      bnc1.put("osname", new AbstractParameter("QNX"));
      bnc1.put("osversion", new AbstractParameter("3.1"));
      bundleNativeCode.add(new AbstractPackageAttribute("/lib/http.DLL", bnc1, null));
      Map<String, Parameter> bnc2 = new HashMap<String, Parameter>();
      bnc2.put("osname", new AbstractParameter("QWE"));
      bnc2.put("osversion", new AbstractParameter("4.0"));
      bundleNativeCode.add(new AbstractPackageAttribute("/lib/tcp.DLL", bnc2, null));
      bundleNativeCode.add(new AbstractPackageAttribute("/lib/iiop.DLL", bnc2, null));
      List<ParameterizedAttribute> metadataBNC = metaData.getBundleNativeCode();
      assertNotNull(metadataBNC);
      assertEquals(bundleNativeCode.size(), metadataBNC.size());
      for (int i = 0; i < metadataBNC.size(); i++)
      {
         ParameterizedAttribute paMD = metadataBNC.get(i);
         ParameterizedAttribute myPA = bundleNativeCode.get(i);
         assertEquals(paMD.getAttribute(), myPA.getAttribute());
         assertEquals(paMD.getAttributes(), myPA.getAttributes());
      }

      List<PackageAttribute> dynamicImports = new ArrayList<PackageAttribute>();
      Map<String, Parameter> dyna1 = new HashMap<String, Parameter>();
      dyna1.put("user", new AbstractParameter("alesj"));
      dynamicImports.add(new AbstractPackageAttribute("org.jboss.test", dyna1, null));
      Map<String, Parameter> dyna2 = new HashMap<String, Parameter>();
      dyna2.put("version", new AbstractParameter("1.2.3.GA"));
      dynamicImports.add(new AbstractPackageAttribute("com.acme.plugin.*", dyna2, null));
      Map<String, Parameter> dyna3 = new HashMap<String, Parameter>();
      dyna3.put("test", new AbstractParameter("test"));
      dynamicImports.add(new AbstractPackageAttribute("*", dyna3, null));
      List<PackageAttribute> metadataDyna = metaData.getDynamicImports();
      assertNotNull(metadataDyna);
      assertEquals(dynamicImports.size(), metadataDyna.size());
      for (int i = 0; i < metadataDyna.size(); i++)
      {
         PackageAttribute paMD = metadataDyna.get(i);
         PackageAttribute myPA = dynamicImports.get(i);
         assertEquals(paMD.getAttribute(), myPA.getAttribute());
         assertEquals(paMD.getPackageInfo(), myPA.getPackageInfo());
         assertEquals(paMD.getAttributes(), myPA.getAttributes());
      }

      List<PackageAttribute> exportPackages = new ArrayList<PackageAttribute>();
      Map<String, Parameter> ep1 = new HashMap<String, Parameter>();
      ep1.put("version", new AbstractParameter("1.3"));
      exportPackages.add(new AbstractPackageAttribute("org.osgi.util.tracker", ep1, null));
      exportPackages.add(new AbstractPackageAttribute("net.osgi.foo", ep1, null));
      Map<String, Parameter> ep2 = new HashMap<String, Parameter>();
      ep2.put("version", new AbstractParameter("[1.0,2.0)"));
      exportPackages.add(new AbstractPackageAttribute("org.jboss.test", ep2, null));
      List<PackageAttribute> metadataExport = metaData.getExportPackages();
      assertNotNull(metadataExport);
      assertEquals(exportPackages.size(), metadataExport.size());
      for (int i = 0; i < metadataExport.size(); i++)
      {
         PackageAttribute paMD = metadataExport.get(i);
         PackageAttribute myPA = exportPackages.get(i);
         assertEquals(paMD.getAttribute(), myPA.getAttribute());
         assertEquals(paMD.getPackageInfo(), myPA.getPackageInfo());
         assertEquals(paMD.getAttributes(), myPA.getAttributes());
         OSGiParameters o1 = new OSGiParameters(paMD.getAttributes());
         OSGiParameters o2 = new OSGiParameters(myPA.getAttributes());
         VersionRange v1 = o1.getVersion();
         VersionRange v2 = o2.getVersion();
         assertEquals(v1, v2);
      }

      Map<String, Parameter> parameters = new HashMap<String, Parameter>();
      parameters.put("bundle-version", new AbstractParameter("\"[3.0.0,4.0.0)\""));
      ParameterizedAttribute fragmentHost = new AbstractParameterizedAttribute("org.eclipse.swt", parameters, null);
      ParameterizedAttribute metadataFragment = metaData.getFragmentHost();
      assertNotNull(metadataFragment);
      assertEquals(fragmentHost.getAttribute(), metadataFragment.getAttribute());
      OSGiParameters o1 = new OSGiParameters(fragmentHost.getAttributes());
      OSGiParameters o2 = new OSGiParameters(metadataFragment.getAttributes());
      VersionRange v1 = o1.getBundleVersion();
      VersionRange v2 = o2.getBundleVersion();
      assertNotNull(v1);
      assertNotNull(v2);
      assertEquals(v1, v2);

      List<PackageAttribute> importPackages = new ArrayList<PackageAttribute>();
      Map<String, Parameter> ip1 = new HashMap<String, Parameter>();
      ip1.put("version", new AbstractParameter("1.4"));
      ip1.put("name", new AbstractParameter("osgi"));
      importPackages.add(new AbstractPackageAttribute("org.osgi.util.tracker", ip1, null));
      importPackages.add(new AbstractPackageAttribute("org.osgi.service.io", ip1, null));
      Map<String, Parameter> ip2 = new HashMap<String, Parameter>();
      ip2.put("version", new AbstractParameter("[2.0,3.0)"));
      Map<String, Parameter> ip2d = new HashMap<String, Parameter>();
      ip2d.put("resolution", new AbstractParameter("osgi-int"));
      importPackages.add(new AbstractPackageAttribute("org.jboss.test", ip2, ip2d));
      List<PackageAttribute> metadataImport = metaData.getImportPackages();
      assertNotNull(metadataImport);
      assertEquals(importPackages.size(), metadataImport.size());
      for (int i = 0; i < metadataImport.size(); i++)
      {
         PackageAttribute paMD = metadataImport.get(i);
         PackageAttribute myPA = importPackages.get(i);
         assertEquals(paMD.getAttribute(), myPA.getAttribute());
         assertEquals(paMD.getPackageInfo(), myPA.getPackageInfo());
         assertEquals(paMD.getAttributes(), myPA.getAttributes());
         assertEquals(paMD.getDirectives(), myPA.getDirectives());
         OSGiParameters oi1 = new OSGiParameters(paMD.getAttributes());
         OSGiParameters oi2 = new OSGiParameters(myPA.getAttributes());
         VersionRange vi1 = oi1.getVersion();
         VersionRange vi2 = oi2.getVersion();
         assertEquals(vi1, vi2);
      }

      List<ParameterizedAttribute> requireBundles = new ArrayList<ParameterizedAttribute>();
      Map<String, Parameter> rb1 = new HashMap<String, Parameter>();
      rb1.put("visibility", new AbstractParameter("true"));
      requireBundles.add(new AbstractParameterizedAttribute("com.acme.chess", null, rb1));
      Map<String, Parameter> rb2 = new HashMap<String, Parameter>();
      rb2.put("bundle-version", new AbstractParameter("1.2"));
      requireBundles.add(new AbstractParameterizedAttribute("com.alesj.test", rb2, null));
      List<ParameterizedAttribute> metadataRB = metaData.getRequireBundles();
      assertNotNull(metadataRB);
      assertEquals(requireBundles.size(), metadataRB.size());
      for (int i = 0; i < metadataRB.size(); i++)
      {
         ParameterizedAttribute paMD = metadataRB.get(i);
         ParameterizedAttribute myPA = requireBundles.get(i);
         assertEquals(paMD.getAttribute(), myPA.getAttribute());
         assertEquals(paMD.getAttributes(), myPA.getAttributes());
         assertEquals(paMD.getDirectives(), myPA.getDirectives());
         OSGiParameters oi1 = new OSGiParameters(paMD.getAttributes());
         OSGiParameters oi2 = new OSGiParameters(myPA.getAttributes());
         String vis1 = oi1.getVisibility();
         String vis2 = oi2.getVisibility();
         assertEquals(vis1, vis2);
         VersionRange vr1 = oi1.getBundleVersion();
         VersionRange vr2 = oi2.getBundleVersion();
         assertEquals(vr1, vr2);
      }
   }
}
