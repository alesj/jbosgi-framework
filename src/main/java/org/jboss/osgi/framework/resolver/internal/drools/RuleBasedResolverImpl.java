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
package org.jboss.osgi.framework.resolver.internal.drools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.resolver.AbstractResolver;
import org.jboss.osgi.framework.resolver.ExportPackage;
import org.jboss.osgi.framework.resolver.ImportPackage;
import org.jboss.osgi.framework.resolver.ResolverBundle;
import org.jboss.osgi.framework.resolver.internal.ResolverSystemBundleImpl;
import org.osgi.framework.Bundle;

/**
 * A Resolver that is based on the Drools rule engine.
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public class RuleBasedResolverImpl extends AbstractResolver
{
   // Provide logging
   public final Logger log = Logger.getLogger(RuleBasedResolverImpl.class);

   private StatefulKnowledgeSession ksession;
   private Map<ResolverBundle, FactHandle> facts = new ConcurrentHashMap<ResolverBundle, FactHandle>();

   public RuleBasedResolverImpl(OSGiBundleManager bundleManager)
   {
      super(bundleManager);

      KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
      kbuilder.add(ResourceFactory.newClassPathResource("META-INF/resolver-rules.drl", getClass()), ResourceType.DRL);
      if (kbuilder.hasErrors())
         throw new IllegalStateException("Cannot create knowledge base" + kbuilder.getErrors());

      KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
      kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
      ksession = kbase.newStatefulKnowledgeSession();
      ksession.setGlobal("log", log);

   }

   @Override
   public ResolverBundle addBundle(Bundle bundle)
   {
      if (bundle == null)
         throw new IllegalArgumentException("Null bundle");
      
      ResolverBundle resBundle;
      if (bundle.getBundleId() == 0)
      {
         resBundle = new ResolverSystemBundleImpl(bundle);
         
         // Insert the system bundle as a fact
         FactHandle factHandle = ksession.insert(resBundle);
         facts.put(resBundle, factHandle);
         
         // Fire all the rules
         ksession.fireAllRules();
      }
      else
      {
         resBundle = super.addBundle(bundle);
      }
      return resBundle;
   }

   @Override
   public ResolverBundle removeBundle(Bundle bundle)
   {
      if (bundle == null)
         throw new IllegalArgumentException("Null bundle");

      ResolverBundle resBundle = super.removeBundle(bundle);
      FactHandle factHandle = facts.get(resBundle);
      if (factHandle != null)
      {
         ksession.update(factHandle, resBundle);
         ksession.fireAllRules();
         ksession.retract(factHandle);
      }
      return resBundle;
   }

   public List<ResolverBundle> resolve(List<Bundle> bundles)
   {
      // Get the list of unresolved resBundles
      List<ResolverBundle> unresolved = new ArrayList<ResolverBundle>();
      if (bundles == null)
      {
         for (ResolverBundle aux : getBundles())
         {
            if (aux.isResolved() == false)
               unresolved.add(aux);
         }
      }
      else
      {
         for (Bundle bundle : bundles)
         {
            ResolverBundle aux = getBundle(bundle);
            if (aux == null)
               throw new IllegalStateException("Cannot obtain resBundle for: " + bundle);

            if (aux.isResolved() == false)
               unresolved.add(aux);
         }
      }

      // Insert the missing resBundles into the knowledge base
      for (ResolverBundle aux : unresolved)
      {
         FactHandle factHandle = ksession.getFactHandle(aux);
         if (factHandle == null)
         {
            factHandle = ksession.insert(aux);
            facts.put(aux, factHandle);
         }
      }

      // Fire all the rules
      ksession.fireAllRules();

      // Return teh list of resolved bundles
      List<ResolverBundle> resolved = new ArrayList<ResolverBundle>();
      for (ResolverBundle aux : getBundles())
      {
         if (aux.isResolved())
            resolved.add(aux);
      }
      return Collections.unmodifiableList(resolved);
   }

   public ExportPackage getExporter(Bundle importer, String packageName)
   {
      if (importer == null)
         throw new IllegalArgumentException("Null importer");
      if (packageName == null)
         throw new IllegalArgumentException("Null packageName");
      
      ResolverBundle resBundle = getBundle(importer);
      if (resBundle == null)
         throw new IllegalStateException("Cannot find resovable for: " + importer);

      ImportPackage importPackage = resBundle.getImportPackage(packageName);
      if (importPackage == null)
         throw new IllegalStateException("Cannot find import package: " + packageName);

      ExportPackage exporter = importPackage.getExporter();
      return exporter;
   }
}