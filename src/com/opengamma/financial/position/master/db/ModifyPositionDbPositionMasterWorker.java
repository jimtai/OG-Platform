/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.position.master.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.IncorrectUpdateSemanticsDataAccessException;

import com.opengamma.DataNotFoundException;
import com.opengamma.financial.position.master.PositionDocument;
import com.opengamma.id.Identifier;
import com.opengamma.id.UniqueIdentifiables;
import com.opengamma.id.UniqueIdentifier;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.db.DbMapSqlParameterSource;
import com.opengamma.util.time.DateUtil;

/**
 * Position master worker to modify a position.
 */
public class ModifyPositionDbPositionMasterWorker extends DbPositionMasterWorker {
  // TODO: transactions

  /** Logger. */
  private static final Logger s_logger = LoggerFactory.getLogger(ModifyPositionDbPositionMasterWorker.class);

  /**
   * Creates an instance.
   */
  public ModifyPositionDbPositionMasterWorker() {
    super();
  }

  //-------------------------------------------------------------------------
  @Override
  protected PositionDocument addPosition(final PositionDocument document) {
    s_logger.debug("addPosition {}", document);
    
    // insert new row
    final Instant now = Instant.now(getTimeSource());
    document.setVersionFromInstant(now);
    document.setVersionToInstant(null);
    document.setCorrectionFromInstant(now);
    document.setCorrectionToInstant(null);
    document.setParentNodeId(document.getParentNodeId().toLatest());
    document.setPortfolioId(checkNodeGetPortfolioOid(document));
    document.setPositionId(null);
    insertPosition(document);
    return document;
  }

  //-------------------------------------------------------------------------
  @Override
  protected PositionDocument updatePosition(final PositionDocument document) {
    UniqueIdentifier uid = document.getPositionId();
    ArgumentChecker.isTrue(uid.isVersioned(), "UniqueIdentifier must be versioned");
    s_logger.debug("updatePosition {}", document);
    
    // load old row
    final PositionDocument oldDoc = getPositionCheckLatestVersion(uid);
    // update old row
    final Instant now = Instant.now(getTimeSource());
    oldDoc.setVersionToInstant(now);
    updateVersionToInstant(oldDoc);
    // insert new row
    document.setVersionFromInstant(now);
    document.setVersionToInstant(null);
    document.setCorrectionFromInstant(now);
    document.setCorrectionToInstant(null);
    document.setParentNodeId(oldDoc.getParentNodeId());
    document.setPortfolioId(oldDoc.getPortfolioId());
    document.setPositionId(oldDoc.getPositionId().toLatest());
    insertPosition(document);
    return document;
  }

  //-------------------------------------------------------------------------
  @Override
  protected void removePosition(final UniqueIdentifier uid) {
    ArgumentChecker.isTrue(uid.isVersioned(), "UniqueIdentifier must be versioned");
    s_logger.debug("removePosition {}", uid);
    
    // load old row
    final PositionDocument oldDoc = getPositionCheckLatestVersion(uid);
    // update old row
    final Instant now = Instant.now(getTimeSource());
    oldDoc.setVersionToInstant(now);
    updateVersionToInstant(oldDoc);
  }

  //-------------------------------------------------------------------------
  @Override
  protected PositionDocument correctPosition(PositionDocument document) {
    UniqueIdentifier uid = document.getPositionId();
    ArgumentChecker.isTrue(uid.isVersioned(), "UniqueIdentifier must be versioned");
    s_logger.debug("correctPosition {}", document);
    
    // load old row
    final PositionDocument oldDoc = getPositionCheckLatestCorrection(uid);
    // update old row
    final Instant now = Instant.now(getTimeSource());
    oldDoc.setCorrectionToInstant(now);
    updateCorrectionToInstant(oldDoc);
    // insert new row
    document.setVersionFromInstant(oldDoc.getVersionFromInstant());
    document.setVersionToInstant(oldDoc.getVersionToInstant());
    document.setCorrectionFromInstant(now);
    document.setCorrectionToInstant(null);
    document.setParentNodeId(oldDoc.getParentNodeId());
    document.setPortfolioId(oldDoc.getPortfolioId());
    document.setPositionId(oldDoc.getPositionId().toLatest());
    insertPosition(document);
    return document;
  }

  //-------------------------------------------------------------------------
  /**
   * Validates that the node exists, returning the associated portfolio oid.
   * @param document  the document, not null
   * @return the portfolio oid, not null
   */
  protected UniqueIdentifier checkNodeGetPortfolioOid(final PositionDocument document) {
    final UniqueIdentifier nodeOid = document.getParentNodeId().toLatest();
    final DbMapSqlParameterSource args = new DbMapSqlParameterSource()
      .addValue("node_oid", nodeOid.getValue())
      .addTimestamp("version_as_of_instant", document.getVersionFromInstant())
      .addTimestamp("corrected_to_instant", document.getCorrectionFromInstant());
    final List<Map<String, Object>> data = getTemplate().queryForList(sqlSelectCheckNodeGetPortfolioOid(), args);
    if (data.size() == 0) {
      throw new DataNotFoundException("Parent node not found: " + nodeOid);
    }
    if (data.size() > 1 || data.get(0).containsKey("PORTFOLIO_OID") == false) {
      throw new IncorrectResultSizeDataAccessException("Parent node table invalid: " + nodeOid, 1, data.size());
    }
    final long portfolioOid = (Long) data.get(0).get("PORTFOLIO_OID");
    return createObjectIdentifier(portfolioOid, null);
  }

  /**
   * Gets the SQL that checks if the portfolio and node exists.
   * @return the SQL, not null
   */
  protected String sqlSelectCheckNodeGetPortfolioOid() {
    return "SELECT DISTINCT f.oid AS portfolio_oid " +
            "FROM pos_node n, pos_portfolio f " +
            "WHERE n.portfolio_id = f.id " +
              "AND n.oid = :node_oid " +
              "AND (f.ver_from_instant <= :version_as_of_instant AND f.ver_to_instant > :version_as_of_instant) " +
              "AND (f.corr_from_instant <= :corrected_to_instant AND f.corr_to_instant > :corrected_to_instant) ";
  }

  //-------------------------------------------------------------------------
  /**
   * Inserts a position.
   * @param document  the document, not null
   */
  protected void insertPosition(final PositionDocument document) {
    final long positionId = nextId();
    final long positionOid = (document.getPositionId() != null ? Long.parseLong(document.getPositionId().getValue()) : positionId);
    // the arguments for inserting into the position table
    final DbMapSqlParameterSource positionArgs = new DbMapSqlParameterSource()
      .addValue("position_id", positionId)
      .addValue("position_oid", positionOid)
      .addValue("portfolio_oid", document.getPortfolioId().toLatest().getValue())
      .addValue("parent_node_oid", document.getParentNodeId().toLatest().getValue())
      .addTimestamp("ver_from_instant", document.getVersionFromInstant())
      .addTimestampNullFuture("ver_to_instant", document.getVersionToInstant())
      .addTimestamp("corr_from_instant", document.getCorrectionFromInstant())
      .addTimestampNullFuture("corr_to_instant", document.getCorrectionToInstant())
      .addValue("quantity", document.getPosition().getQuantity());
    // the arguments for inserting into the seckey table
    final List<DbMapSqlParameterSource> secKeyList = new ArrayList<DbMapSqlParameterSource>();
    for (Identifier id : document.getPosition().getSecurityKey()) {
      final long secKeyId = nextId();
      final DbMapSqlParameterSource treeArgs = new DbMapSqlParameterSource()
        .addValue("seckey_id", secKeyId)
        .addValue("position_id", positionId)
        .addValue("id_scheme", id.getScheme().getName())
        .addValue("id_value", id.getValue());
      secKeyList.add(treeArgs);
    }
    getTemplate().update(sqlInsertPosition(), positionArgs);
    getTemplate().batchUpdate(sqlInsertSecurityKey(), (DbMapSqlParameterSource[]) secKeyList.toArray(new DbMapSqlParameterSource[secKeyList.size()]));
    // set the uid
    final UniqueIdentifier uid = createUniqueIdentifier(positionOid, positionId, null);
    UniqueIdentifiables.setInto(document.getPosition(), uid);
    document.setPositionId(uid);
  }

  /**
   * Gets the SQL for inserting a position.
   * @return the SQL, not null
   */
  protected String sqlInsertPosition() {
    return "INSERT INTO pos_position " +
              "(id, oid, portfolio_oid, parent_node_oid, ver_from_instant, ver_to_instant, corr_from_instant, corr_to_instant, quantity) " +
            "VALUES " +
              "(:position_id, :position_oid, :portfolio_oid, :parent_node_oid, :ver_from_instant, :ver_to_instant, :corr_from_instant, :corr_to_instant, :quantity)";
  }

  /**
   * Gets the SQL for inserting a security key.
   * @return the SQL, not null
   */
  protected String sqlInsertSecurityKey() {
    return "INSERT INTO pos_securitykey " +
              "(id, position_id, id_scheme, id_value) " +
            "VALUES " +
              "(:seckey_id, :position_id, :id_scheme, :id_value)";
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the position document ensuring that it is the latest version.
   * @param uid  the unique identifier to load, not null
   * @return the loaded document, not null
   */
  protected PositionDocument getPositionCheckLatestVersion(final UniqueIdentifier uid) {
    final PositionDocument oldDoc = getMaster().getPosition(uid);  // checks uid exists
    if (oldDoc.getVersionToInstant() != null) {
      throw new IllegalArgumentException("UniqueIdentifier is not latest version: " + uid);
    }
    return oldDoc;
  }

  /**
   * Updates the document row to mark the version as ended.
   * @param document  the document to update, not null
   */
  protected void updateVersionToInstant(final PositionDocument document) {
    final DbMapSqlParameterSource args = new DbMapSqlParameterSource()
      .addValue("position_id", document.getPositionId().getVersion())
      .addTimestamp("ver_to_instant", document.getVersionToInstant())
      .addValue("max_instant", DateUtil.MAX_SQL_TIMESTAMP);
    int rowsUpdated = getTemplate().update(sqlUpdateVersionToInstant(), args);
    if (rowsUpdated != 1) {
      throw new IncorrectUpdateSemanticsDataAccessException("Update end version instant failed, rows updated: " + rowsUpdated);
    }
  }

  /**
   * Gets the SQL for updating the end version of a position.
   * @return the SQL, not null
   */
  protected String sqlUpdateVersionToInstant() {
    return "UPDATE pos_position " +
              "SET ver_to_instant = :ver_to_instant " +
            "WHERE id = :position_id " +
              "AND ver_to_instant = :max_instant ";
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the position document ensuring that it is the latest version.
   * @param uid  the unique identifier to load, not null
   * @return the loaded document, not null
   */
  protected PositionDocument getPositionCheckLatestCorrection(final UniqueIdentifier uid) {
    final PositionDocument oldDoc = getMaster().getPosition(uid);  // checks uid exists
    if (oldDoc.getCorrectionToInstant() != null) {
      throw new IllegalArgumentException("UniqueIdentifier is not latest correction: " + uid);
    }
    return oldDoc;
  }

  /**
   * Updates the document row to mark the correction as ended.
   * @param document  the document to update, not null
   */
  protected void updateCorrectionToInstant(final PositionDocument document) {
    final DbMapSqlParameterSource args = new DbMapSqlParameterSource()
      .addValue("position_id", document.getPositionId().getVersion())
      .addTimestamp("corr_to_instant", document.getCorrectionToInstant())
      .addValue("max_instant", DateUtil.MAX_SQL_TIMESTAMP);
    int rowsUpdated = getTemplate().update(sqlUpdateCorrectionToInstant(), args);
    if (rowsUpdated != 1) {
      throw new IncorrectUpdateSemanticsDataAccessException("Update end correction instant failed, rows updated: " + rowsUpdated);
    }
  }

  /**
   * Gets the SQL for updating the end correction of a position.
   * @return the SQL, not null
   */
  protected String sqlUpdateCorrectionToInstant() {
    return "UPDATE pos_position " +
              "SET corr_to_instant = :corr_to_instant " +
            "WHERE id = :position_id " +
              "AND corr_to_instant = :max_instant ";
  }

}
