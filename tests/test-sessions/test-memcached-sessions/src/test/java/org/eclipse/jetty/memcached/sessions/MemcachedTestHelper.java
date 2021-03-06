//
// ========================================================================
// Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under
// the terms of the Eclipse Public License 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0
//
// This Source Code may also be made available under the following
// Secondary Licenses when the conditions for such availability set
// forth in the Eclipse Public License, v. 2.0 are satisfied:
// the Apache License v2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.memcached.sessions;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.memcached.session.MemcachedSessionDataMapFactory;
import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.AbstractSessionDataStoreFactory;
import org.eclipse.jetty.server.session.CachingSessionDataStoreFactory;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionDataStoreFactory;
import org.eclipse.jetty.server.session.SessionHandler;

/**
 * MemcachedTestHelper
 */
public class MemcachedTestHelper
{

    public static class MockDataStore extends AbstractSessionDataStore
    {
        private Map<String, SessionData> _store = new HashMap<>();
        private int _loadCount = 0;

        @Override
        public boolean isPassivating()
        {
            return true;
        }

        @Override
        public boolean exists(String id) throws Exception
        {
            return _store.get(id) != null;
        }

        @Override
        public SessionData doLoad(String id) throws Exception
        {
            _loadCount++;
            return _store.get(id);
        }

        public void zeroLoadCount()
        {
            _loadCount = 0;
        }

        public int getLoadCount()
        {
            return _loadCount;
        }

        @Override
        public boolean delete(String id) throws Exception
        {
            return (_store.remove(id) != null);
        }

        @Override
        public void doStore(String id, SessionData data, long lastSaveTime) throws Exception
        {
            _store.put(id, data);
        }

        @Override
        public Set<String> doGetExpired(Set<String> candidates)
        {
            Set<String> expiredIds = new HashSet<>();
            long now = System.currentTimeMillis();
            if (candidates != null)
            {
                for (String id : candidates)
                {
                    SessionData sd = _store.get(id);
                    if (sd == null)
                        expiredIds.add(id);
                    else if (sd.isExpiredAt(now))
                        expiredIds.add(id);
                }
            }

            for (String id : _store.keySet())
            {
                SessionData sd = _store.get(id);
                if (sd.isExpiredAt(now))
                    expiredIds.add(id);
            }

            return expiredIds;
        }

        @Override
        protected void doStop() throws Exception
        {
            super.doStop();
        }
    }

    public static class MockDataStoreFactory extends AbstractSessionDataStoreFactory
    {

        @Override
        public SessionDataStore getSessionDataStore(SessionHandler handler) throws Exception
        {
            return new MockDataStore();
        }
    }

    public static SessionDataStoreFactory newSessionDataStoreFactory()
    {
        MockDataStoreFactory storeFactory = new MockDataStoreFactory();
        MemcachedSessionDataMapFactory mapFactory = new MemcachedSessionDataMapFactory();
        mapFactory.setAddresses(new InetSocketAddress("localhost", 11211));

        CachingSessionDataStoreFactory factory = new CachingSessionDataStoreFactory();
        factory.setSessionDataMapFactory(mapFactory);
        factory.setSessionStoreFactory(storeFactory);
        return factory;
    }
}
