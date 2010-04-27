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

package org.jboss.osgi.framework.metadata.internal;

import org.jboss.beans.metadata.plugins.AbstractBeanMetaData;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.managed.api.annotation.ManagementObject;
import org.jboss.managed.api.annotation.ManagementProperties;
import org.jboss.xb.annotations.JBossXmlSchema;

import javax.xml.bind.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Simplify pojo 2 osgi metadata.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@ManagementObject(properties = ManagementProperties.EXPLICIT)
@JBossXmlSchema(namespace="urn:jboss:pojo2osgi:1.0", elementFormDefault= XmlNsForm.QUALIFIED)
@XmlRootElement(name="osgi")
@XmlType(name="osgiType", propOrder={"exposedTypes", "aliasMetaData", "related", "annotations", "classLoader", "constructor", "properties", "create", "start", "stop", "destroy", "depends", "demands", "supplies", "installs", "uninstalls", "installCallbacks", "uninstallCallbacks"})
public class OSGiPojoMetaData extends AbstractBeanMetaData
{
   private Set<String> exposedTypes;
   private String mdrService = "MDRService";
   private volatile boolean initialized;

   public Set<String> getExposedTypes()
   {
      return exposedTypes;
   }

   @XmlElement(name="exposed-type")
   public void setExposedTypes(Set<String> exposedTypes)
   {
      this.exposedTypes = exposedTypes;
   }

   public String getMdrService()
   {
      return mdrService;
   }

   @XmlAttribute(name = "MDRService")
   public void setMdrService(String mdrService)
   {
      this.mdrService = mdrService;
   }

   @Override
   public List<BeanMetaData> getBeans()
   {
      if (initialized == false)
      {
         BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(this);
         if (exposedTypes != null && exposedTypes.isEmpty() == false)
         {
            for (String exposedType : exposedTypes)
            {
               builder.addRelatedClass(exposedType, "OSGi");
            }
         }
         if (mdrService != null)
            builder.addDemand(mdrService, ControllerState.PRE_INSTALL, null);

         initialized = true;
      }
      return Collections.<BeanMetaData>singletonList(this);
   }
}
