/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.master;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Instant;

import com.opengamma.DataNotFoundException;
import com.opengamma.core.change.BasicChangeManager;
import com.opengamma.core.change.ChangeEvent;
import com.opengamma.core.change.ChangeListener;
import com.opengamma.core.change.ChangeManager;
import com.opengamma.id.ObjectId;
import com.opengamma.id.ObjectIdentifiable;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.ehcache.EHCacheUtils;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;

/**
 * A cache decorating a master, mainly intended to reduce the frequency and repetition of queries to the underlying
 * master.
 * <p>
 * The cache is implemented using {@code EHCache}.
 *
 * TODO Fix change management
 * TODO Cache misses too
 * TODO implement correct, replace etc.
 *
 * @param <D> the document type returned by the master
 */
public abstract class AbstractEHCachingMaster<D extends AbstractDocument> implements AbstractChangeProvidingMaster<D> {

  /** Logger. */
  private static final Logger s_logger = LoggerFactory.getLogger(AbstractEHCachingMaster.class);
  /** The underlying cache. */
  private final AbstractChangeProvidingMaster<D> _underlying;
  /** The cache manager. */
  private final CacheManager _cacheManager;
  /** Listens for changes in the underlying security source. */
  private final ChangeListener _changeListener;
  /** The local change manager. */
  private final ChangeManager _changeManager;
  /** The document cache indexed by ObjectId/version/correction. */
  private final Ehcache _oidToUidCache;
  /** The document cache indexed by UniqueId. */
  private final Ehcache _uidToDocumentCache;
  /** The document by oid cache's name. */
  private final String _oidtoUidCacheName = getClass().getName() + "-oidToUidCache";
  /** The document by uid cache's name. */
  private final String _uidToDocumentCacheName = getClass().getName() + "-uidToDocumentCache";

  private class UidToDocumentCacheEntryFactory implements CacheEntryFactory {

    @Override
    public Object createEntry(Object key) throws Exception {
      return null;  // TODO
    }

  }

  /**
   * Creates an instance over an underlying source specifying the cache manager.
   *
   * @param underlying  the underlying source, not null
   * @param cacheManager  the cache manager, not null
   */
  public AbstractEHCachingMaster(final AbstractChangeProvidingMaster<D> underlying, final CacheManager cacheManager) {
    ArgumentChecker.notNull(underlying, "underlying");
    ArgumentChecker.notNull(cacheManager, "cacheManager");

    _underlying = underlying;
    _cacheManager = cacheManager;

    EHCacheUtils.addCache(cacheManager, _oidtoUidCacheName);
    _oidToUidCache = EHCacheUtils.getCacheFromManager(cacheManager, _oidtoUidCacheName);
    EHCacheUtils.addCache(cacheManager, _uidToDocumentCacheName);
    _uidToDocumentCache = EHCacheUtils.getCacheFromManager(cacheManager, _uidToDocumentCacheName);

    //_uidToDocumentCache = new SelfPopulatingCache(new Cache(), new UidToDocumentCacheEntryFactory());
    //_cacheManager.addCache(_uidToDocumentCache);
    //_oidToUidCache = new SelfPopulatingCache();
    //_cacheManager.addCache(_oidToUidCache);

    _changeManager = new BasicChangeManager();
    _changeListener = new ChangeListener() {
      @Override
      public void entityChanged(ChangeEvent event) {
        final ObjectId oid = event.getObjectId();
        final Instant versionFrom = event.getVersionFrom();
        final Instant versionTo = event.getVersionTo();
        cleanCaches(oid, versionFrom, versionTo);
        _changeManager.entityChanged(event.getType(), event.getObjectId(),
            event.getVersionFrom(), event.getVersionTo(), event.getVersionInstant());
      }
    };
    underlying.changeManager().addChangeListener(_changeListener);
  }

  //-------------------------------------------------------------------------

  /**
   * Map from fromVersion instants to fromCorrection instants to documents
   */
  private class InstantMap {

    /** the version/correction/document map */
    private final NavigableMap<Instant, NavigableMap<Instant, D>> _fromVersionCorrectionInstantMap;

    public InstantMap() {
      _fromVersionCorrectionInstantMap = new TreeMap<>();
    }

    public InstantMap(NavigableMap<Instant, NavigableMap<Instant, D>> map) {
      _fromVersionCorrectionInstantMap = map;
    }

    public D get(VersionCorrection versionCorrection) {
      versionCorrection = versionCorrection.withLatestFixed(Instant.now());
      Instant fromVersionInstant = _fromVersionCorrectionInstantMap.floorKey(versionCorrection.getVersionAsOf());
      if (fromVersionInstant != null) {
        NavigableMap<Instant, D> fromCorrectionInstantMap = _fromVersionCorrectionInstantMap.get(fromVersionInstant);
        Instant fromCorrectionInstant = fromCorrectionInstantMap.floorKey(versionCorrection.getCorrectedTo());
        if (fromCorrectionInstant != null) {
          D document = fromCorrectionInstantMap.get(fromCorrectionInstant);
          if ((
               document.getVersionToInstant() == null ||
               document.getVersionToInstant().equals(versionCorrection.getVersionAsOf()) ||
               document.getVersionToInstant().isAfter(versionCorrection.getVersionAsOf())
             ) && (
               document.getCorrectionToInstant() == null ||
               document.getCorrectionToInstant().equals(versionCorrection.getCorrectedTo()) ||
               document.getCorrectionToInstant().isAfter(versionCorrection.getCorrectedTo())
             )) {
            return document;
          } // else one or both of the found version and correction expire too early
        } // else did not find a correction that's old enough
      } // else did not find a document that's old enough
      return null;
    }

    public InstantMap getRange(Instant fromVersion, Instant toVersion) {

      // get tail of map
      NavigableMap<Instant, NavigableMap<Instant, D>> tailMap =
          fromVersion != null && _fromVersionCorrectionInstantMap.floorKey(fromVersion) != null
          ? _fromVersionCorrectionInstantMap.tailMap(_fromVersionCorrectionInstantMap.floorKey(fromVersion), true)
          : _fromVersionCorrectionInstantMap;

      // get head of tail
      NavigableMap<Instant, NavigableMap<Instant, D>> headOfTailMap =
          toVersion != null && _fromVersionCorrectionInstantMap.floorKey(toVersion) != null
          ? tailMap.headMap(_fromVersionCorrectionInstantMap.floorKey(toVersion), false)
          : tailMap;

      return new InstantMap(headOfTailMap);
    }

    public void put(D document) { // assumes document has the same objectid as the others in this InstantMap
      Instant versionFromInstant = document.getVersionFromInstant() != null ? document.getVersionFromInstant() : Instant.EPOCH;
      Instant correctionFromInstant = document.getCorrectionFromInstant() != null ? document.getCorrectionFromInstant() : Instant.EPOCH;
      NavigableMap<Instant, D> fromCorrectionInstantMap = _fromVersionCorrectionInstantMap.get(versionFromInstant);
      if (fromCorrectionInstantMap == null) {
        fromCorrectionInstantMap = new TreeMap<>();
        _fromVersionCorrectionInstantMap.put(versionFromInstant, fromCorrectionInstantMap);
      }
      // TODO may need to invalidate previous latest by changing version/correction or reloading from underlying
      fromCorrectionInstantMap.put(correctionFromInstant, document);
    }

    public NavigableMap<Instant, NavigableMap<Instant, D>> getMap() {
      return _fromVersionCorrectionInstantMap;
    }
  } // InstantMap

  private InstantMap getOrCreateInstantMap(ObjectIdentifiable objectId, Ehcache cache) {
    Element e = cache.get(objectId);
    InstantMap instantMap;
    if (e != null) {
      instantMap = (InstantMap) (e.getObjectValue());
    } else {
      instantMap = new InstantMap();
      cache.put(new Element(objectId, instantMap));
    }
    return instantMap;
  }
  //-------------------------------------------------------------------------

  @Override
  public D get(ObjectIdentifiable objectId, VersionCorrection versionCorrection) {
    ArgumentChecker.notNull(objectId, "objectId");
    ArgumentChecker.notNull(versionCorrection, "versionCorrection");

    // Get/create instant map
    InstantMap instantMap = getOrCreateInstantMap(objectId, getOidToUidCache());

    // Get/create document in instant map
    D result = instantMap.get(versionCorrection);
    if (result != null) {
      s_logger.debug("retrieved object: {} from doc-cache", result);
    } else {
      // Get document from underlying master
      result = getUnderlying().get(objectId, versionCorrection);

      // Update uniqueid map
      if (result != null) { // TODO NOT CACHING MISSES :(
        getUidToDocumentCache().put(new Element(result.getUniqueId(), result));

        // Update objectid/version/correction map
        instantMap.put(result);
      }
    }
    return result;
  }

  @Override
  public D get(UniqueId uniqueId) {
    ArgumentChecker.notNull(uniqueId, "uniqueId");

    D result;
    if (!uniqueId.isVersioned()) {
      // Revert to ObjectId search for unversioned UniqueIds
      result = get(uniqueId.getObjectId(), VersionCorrection.LATEST);
    } else {
      // Locate UniqueId in cache
      Element e = getUidToDocumentCache().get(uniqueId);
      if (e != null) {
        result = (D) (e.getObjectValue());
        s_logger.debug("retrieved object: {} from doc-cache", result);
      } else {
        // Get document from underlying master
        result = getUnderlying().get(uniqueId);

        if (result != null) { // TODO NOT CACHING MISSES :(
          // Update uniqueid map
          getUidToDocumentCache().put(new Element(uniqueId, result));

          // Update objectid/version/correction map
          InstantMap instantMap = getOrCreateInstantMap(uniqueId.getObjectId(), getOidToUidCache());
          instantMap.put(result);
        }
      }
    }
    return result;
  }

  @Override
  public Map<UniqueId, D> get(Collection<UniqueId> uniqueIds) {
    Map<UniqueId, D> result = new HashMap<>();
    for (UniqueId uniqueId : uniqueIds) {
      try {
        D object = get(uniqueId);
        result.put(uniqueId, object);
      } catch (DataNotFoundException ex) {
        // do nothing
      }
    }
    return result;
  }

  //-------------------------------------------------------------------------

  @Override
  public D add(D document) {

    // Add document to underlying master
    D result = getUnderlying().add(document);

    // Store document in ObjectId/version/correction map
    InstantMap instantMap = getOrCreateInstantMap(result.getObjectId(), getOidToUidCache());
    instantMap.put(result);

    // Store document in UniqueId map
    getUidToDocumentCache().put(new Element(result.getUniqueId(), result));

    return result;
  }

  @Override
  public D update(D document) {
    // Update document in underlying master
    D result = getUnderlying().update(document);

    // Store document in ObjectId/version/correction map
    InstantMap instantMap = getOrCreateInstantMap(result.getObjectId(), getOidToUidCache());
    instantMap.put(result);

    // Store document in UniqueId map
    getUidToDocumentCache().put(new Element(result.getUniqueId(), result));

    // TODO adjust version/correction validity of previous version in Oid map
    // TODO do we need to fire entity changed events here?
    return result;
  }

  @Override
  public void remove(ObjectIdentifiable oid) {
    // Remove document from underlying master
    getUnderlying().remove(oid);

    // Adjust version/correction validity of latest version in Oid cache
    cleanCaches(oid.getObjectId(), Instant.now(), null);
  }

  @Override
  public D correct(D document) {
    // Correct document in underlying master
    D result = getUnderlying().correct(document); //TODO

    // Adjust version/correction validity of latest version in Oid cache
//    InstantMap instantMap = getOrCreateInstantMap(document.getObjectId(), getOidToUidCache());
//    D oldDocument = instantMap.get(VersionCorrection.of(document.getVersionToInstant(), document.getCorrectionToInstant()));
//    if (oldDocument != null) {
//      oldDocument.setVersionToInstant();   // hope this is accurate enough, might need to fetch times from underlyig master
//      oldDocument.setCorrectionToInstant();
//    }

    return result;
  }

  @Override
  public List<UniqueId> replaceVersion(UniqueId uniqueId, List<D> replacementDocuments) {
    return getUnderlying().replaceVersion(uniqueId, replacementDocuments); //TODO
  }

  @Override
  public List<UniqueId> replaceAllVersions(ObjectIdentifiable objectId, List<D> replacementDocuments) {
    return getUnderlying().replaceAllVersions(objectId, replacementDocuments); //TODO
  }

  @Override
  public List<UniqueId> replaceVersions(ObjectIdentifiable objectId, List<D> replacementDocuments) {
    return getUnderlying().replaceVersions(objectId, replacementDocuments); //TODO
  }

  @Override
  public UniqueId replaceVersion(D replacementDocument) {
    return getUnderlying().replaceVersion(replacementDocument); //TODO
  }

  @Override
  public void removeVersion(UniqueId uniqueId) {
    getUnderlying().removeVersion(uniqueId); //TODO
  }

  @Override
  public UniqueId addVersion(ObjectIdentifiable objectId, D documentToAdd) {
    return getUnderlying().addVersion(objectId, documentToAdd); //TODO
  }

  //-------------------------------------------------------------------------

  private void cleanCaches(ObjectId oid, Instant fromVersion, Instant toVersion) {

    // Get the documents that match the version range
    InstantMap instantMap = getOrCreateInstantMap(oid, getOidToUidCache()).getRange(fromVersion, toVersion);

    // Remove all matching versions
    for (NavigableMap<Instant, D> correctionMap : instantMap.getMap().values()) {

      // Remove each correction from Uid map
      for (D document : correctionMap.values()) {
        getUidToDocumentCache().remove(document.getUniqueId());
      }

      // Remove all corrections from Oid map
      correctionMap.clear();
    }
  }

  /**
   * Call this at the end of a unit test run to clear the state of EHCache.
   * It should not be part of a generic lifecycle method.
   */
  public void shutdown() {
    getUnderlying().changeManager().removeChangeListener(_changeListener);
    getCacheManager().clearAllStartingWith(_oidtoUidCacheName);
    getCacheManager().clearAllStartingWith(_uidToDocumentCacheName);
    getCacheManager().removeCache(_oidtoUidCacheName);
    getCacheManager().removeCache(_uidToDocumentCacheName);
  }

  //-------------------------------------------------------------------------

  /**
   * Gets the underlying source of items.
   *
   * @return the underlying source of items, not null
   */
  protected AbstractChangeProvidingMaster<D> getUnderlying() {
    return _underlying;
  }

  /**
   * Gets the cache manager.
   *
   * @return the cache manager, not null
   */
  protected CacheManager getCacheManager() {
    return _cacheManager;
  }

  /**
   * Gets the document by ObjectId cache.
   *
   * @return the cache, not null
   */
  protected Ehcache getOidToUidCache() {
    return _oidToUidCache;
  }

  /**
   * Gets the document by UniqueId cache.
   *
   * @return the cache, not null
   */
  protected Ehcache getUidToDocumentCache() {
    return _uidToDocumentCache;
  }

  /**
   * Gets the change manager.
   *
   * @return the change manager, not null
   */
  @Override
  public ChangeManager changeManager() {
    return _changeManager;
  }

  //-------------------------------------------------------------------------

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getUnderlying() + "]";
  }

}