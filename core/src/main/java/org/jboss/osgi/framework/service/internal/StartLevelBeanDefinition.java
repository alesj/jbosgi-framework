/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.osgi.framework.service.internal;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.jboss.beans.metadata.plugins.AbstractBeanMetaData;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.ValueMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.xb.annotations.JBossXmlSchema;
import org.osgi.service.startlevel.StartLevel;

/**
 * Bean Definition for the Start Level Service implementation.
 * 
 * @author <a href="david@redhat.com">David Bosschaert</a>
 */
@ManagementObject(properties = ManagementProperties.EXPLICIT)
@JBossXmlSchema(namespace = "urn:jboss:startlevel:1.0", elementFormDefault = XmlNsForm.QUALIFIED)
@XmlRootElement(name = "start-level")
@XmlType(name = "startLevelType", propOrder = { "constructor", "bundle" })
public class StartLevelBeanDefinition extends AbstractBeanMetaData
{
   private static final long serialVersionUID = 1L;

   private List<StartLevelBeanEntry> bundles;
   private String mdrService = "MDRService";
   private volatile boolean initialized;

   public List<StartLevelBeanEntry> getBundle()
   {
      return bundles;
   }

   @XmlElement(name = "bundle")
   public void setBundle(List<StartLevelBeanEntry> b)
   {
      bundles = b;
   }

   public String getMdrService()
   {
      return mdrService;
   }

   @XmlAttribute(name = "MDRService")
   public void setMdrService(String mdr)
   {
      mdrService = mdr;
   }

   @Override
   public List<BeanMetaData> getBeans()
   {
      if (!initialized)
      {
         BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(this);
         builder.setName("StartLevelService");
         builder.setBean(StartLevelImpl.class.getName());
         builder.addPropertyMetaData("bundleMetaData", getBundle());
         
         ValueMetaData vmd = builder.createInject("OSGiBundleManager");
         builder.addConstructorParameter(OSGiBundleManager.class.getName(), vmd);
         builder.addRelatedClass(StartLevel.class.getName(), "OSGi");
         if (mdrService != null)
            builder.addDemand(mdrService, ControllerState.PRE_INSTALL, null);

         initialized = true;
      }
      return Collections.<BeanMetaData> singletonList(this);
   }
}
