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

import org.jboss.kernel.spi.metadata.KernelMetaDataRepository;
import org.jboss.metadata.spi.loader.MutableMetaDataLoader;
import org.jboss.metadata.spi.repository.MutableMetaDataRepository;
import org.jboss.metadata.spi.retrieval.MetaDataRetrieval;
import org.jboss.metadata.spi.scope.CommonLevels;
import org.jboss.metadata.spi.scope.Scope;
import org.jboss.metadata.spi.scope.ScopeKey;
import org.jboss.metadata.plugins.loader.memory.MemoryMetaDataLoader;

/**
 * Describe osgi service.
 * Put its properties into MDR.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
class DescribeAction extends OSGiServiceAction
{
   protected void installAction(OSGiServiceState context) throws Throwable
   {
      KernelMetaDataRepository repository = getRepository(context);
      ScopeKey contextScopeKey = context.getScopeInfo().getMutableScope();
      Scope scope = contextScopeKey.getScope(CommonLevels.INSTANCE);
      ScopeKey key = new ScopeKey(scope);
      MutableMetaDataRepository mutable = repository.getMetaDataRepository();
      MetaDataRetrieval retrieval = mutable.getMetaDataRetrieval(key);
      if (retrieval == null)
      {
         retrieval = new MemoryMetaDataLoader(key);
         mutable.addMetaDataRetrieval(retrieval);
      }
      else if (retrieval.retrieveMetaData(Dictionary.class) != null)
      {
         return; // we already have Dictionary            
      }

      if (retrieval instanceof MutableMetaDataLoader)
      {
         MutableMetaDataLoader mmdl = (MutableMetaDataLoader)retrieval;
         Dictionary<String, Object> dictionary = new ServiceRefDictionary(context);
         mmdl.addMetaData(dictionary, Dictionary.class);
      }
   }

   protected void uninstallAction(OSGiServiceState context)
   {
   }
}