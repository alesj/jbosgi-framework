/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.osgi.framework.bundle;

import java.util.Dictionary;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.kernel.Kernel;
import org.jboss.metadata.plugins.loader.memory.MemoryMetaDataLoader;
import org.jboss.metadata.spi.repository.MutableMetaDataRepository;
import org.jboss.metadata.spi.retrieval.MetaDataRetrieval;
import org.jboss.metadata.spi.retrieval.MetaDataRetrievalFactory;
import org.jboss.metadata.spi.scope.CommonLevels;
import org.jboss.metadata.spi.scope.Scope;
import org.jboss.metadata.spi.scope.ScopeKey;

/**
 * Add OSGi's dictionary to MDR.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class InstanceMetaDataRetrievalFactory implements MetaDataRetrievalFactory
{
   private Controller controller;
   private MutableMetaDataRepository repository;
   
   @SuppressWarnings("rawtypes")
   private Set<DictionaryFactory> factories = new CopyOnWriteArraySet<DictionaryFactory>();

   public InstanceMetaDataRetrievalFactory(Kernel kernel)
   {
      if (kernel == null)
         throw new IllegalArgumentException("Null kernel");

      this.controller = kernel.getController();
      this.repository = kernel.getMetaDataRepository().getMetaDataRepository();
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   public MetaDataRetrieval getMetaDataRetrieval(Scope scope)
   {
      if (scope == null)
         throw new IllegalArgumentException("Null scope");
      if (CommonLevels.INSTANCE.equals(scope.getScopeLevel()) == false)
         throw new IllegalArgumentException("Not an instance scope: " + scope);

      MemoryMetaDataLoader loader = new MemoryMetaDataLoader(new ScopeKey(scope));
      repository.addMetaDataRetrieval(loader); // remember loader

      Object qualifier = scope.getQualifier();
      ControllerContext context = controller.getContext(qualifier, null);
      if (context != null)
      {
         DictionaryFactory factory = null;
         for (DictionaryFactory df : factories)
         {
            Class<?> contextType = df.getContextType();
            if (contextType.isInstance(context))
            {
               factory = df;
               break;
            }
         }
         if (factory != null)
         {
            Dictionary<String, Object> dictionary = factory.getDictionary(context);
            loader.addMetaData(dictionary, Dictionary.class);
         }
      }
      return loader;
   }

   /**
    * Add dictonary factory.
    *
    * @param factory the factory
    * @return Set#add
    */
   @SuppressWarnings("rawtypes")
   public boolean addFactory(DictionaryFactory factory)
   {
      if (factory == null)
         throw new IllegalArgumentException("Null factory");
      if (factory.getContextType() == null)
         throw new IllegalArgumentException("Null context type on factory: " + factory);

      return factories.add(factory);
   }

   /**
    * Remove dictonary factory.
    *
    * @param factory the factory
    * @return Set#add
    */
   @SuppressWarnings("rawtypes")
   public boolean removeFactory(DictionaryFactory factory)
   {
      if (factory == null)
         throw new IllegalArgumentException("Null factory");

      return factories.remove(factory);
   }
}