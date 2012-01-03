/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.masterdb.batch;

import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectBean;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.time.Instant;
import java.util.Map;

@BeanDefinition
public class RiskValue extends DirectBean {

  @PropertyDefinition
  private long _id;

  @PropertyDefinition
  private long _calculationConfigurationId;

  @PropertyDefinition
  private long _valueNameId;

  @PropertyDefinition
  private long _valueSpecificationId;

  @PropertyDefinition
  private long _functionUniqueId;

  @PropertyDefinition
  private long _computationTargetId;

  @PropertyDefinition
  private long _runId;

  @PropertyDefinition
  private double _value;

  @PropertyDefinition
  private Instant _evalInstant;

  @PropertyDefinition
  private long _computeNodeId;


  public SqlParameterSource toSqlParameterSource() {
    MapSqlParameterSource source = new MapSqlParameterSource();
    source.addValue("id", getId());
    source.addValue("calculation_configuration_id", getCalculationConfigurationId());
    source.addValue("value_name_id", getValueNameId());
    source.addValue("value_specification_id", getValueSpecificationId());
    source.addValue("function_unique_id", getFunctionUniqueId());
    source.addValue("computation_target_id", getComputationTargetId());
    source.addValue("run_id", getRunId());
    source.addValue("value", getValue());
    source.addValue("eval_instant", getEvalInstant());
    source.addValue("compute_node_id", getComputeNodeId());
    return source;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code RiskValue}.
   * @return the meta-bean, not null
   */
  public static RiskValue.Meta meta() {
    return RiskValue.Meta.INSTANCE;
  }
  static {
    JodaBeanUtils.registerMetaBean(RiskValue.Meta.INSTANCE);
  }

  @Override
  public RiskValue.Meta metaBean() {
    return RiskValue.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName, boolean quiet) {
    switch (propertyName.hashCode()) {
      case 3355:  // id
        return getId();
      case 1329751720:  // calculationConfigurationId
        return getCalculationConfigurationId();
      case -1585450537:  // valueNameId
        return getValueNameId();
      case -1127019571:  // valueSpecificationId
        return getValueSpecificationId();
      case 1563911364:  // functionUniqueId
        return getFunctionUniqueId();
      case -1362849421:  // computationTargetId
        return getComputationTargetId();
      case 108875014:  // runId
        return getRunId();
      case 111972721:  // value
        return getValue();
      case 820536741:  // evalInstant
        return getEvalInstant();
      case 398290388:  // computeNodeId
        return getComputeNodeId();
    }
    return super.propertyGet(propertyName, quiet);
  }

  @Override
  protected void propertySet(String propertyName, Object newValue, boolean quiet) {
    switch (propertyName.hashCode()) {
      case 3355:  // id
        setId((Long) newValue);
        return;
      case 1329751720:  // calculationConfigurationId
        setCalculationConfigurationId((Long) newValue);
        return;
      case -1585450537:  // valueNameId
        setValueNameId((Long) newValue);
        return;
      case -1127019571:  // valueSpecificationId
        setValueSpecificationId((Long) newValue);
        return;
      case 1563911364:  // functionUniqueId
        setFunctionUniqueId((Long) newValue);
        return;
      case -1362849421:  // computationTargetId
        setComputationTargetId((Long) newValue);
        return;
      case 108875014:  // runId
        setRunId((Long) newValue);
        return;
      case 111972721:  // value
        setValue((Double) newValue);
        return;
      case 820536741:  // evalInstant
        setEvalInstant((Instant) newValue);
        return;
      case 398290388:  // computeNodeId
        setComputeNodeId((Long) newValue);
        return;
    }
    super.propertySet(propertyName, newValue, quiet);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      RiskValue other = (RiskValue) obj;
      return JodaBeanUtils.equal(getId(), other.getId()) &&
          JodaBeanUtils.equal(getCalculationConfigurationId(), other.getCalculationConfigurationId()) &&
          JodaBeanUtils.equal(getValueNameId(), other.getValueNameId()) &&
          JodaBeanUtils.equal(getValueSpecificationId(), other.getValueSpecificationId()) &&
          JodaBeanUtils.equal(getFunctionUniqueId(), other.getFunctionUniqueId()) &&
          JodaBeanUtils.equal(getComputationTargetId(), other.getComputationTargetId()) &&
          JodaBeanUtils.equal(getRunId(), other.getRunId()) &&
          JodaBeanUtils.equal(getValue(), other.getValue()) &&
          JodaBeanUtils.equal(getEvalInstant(), other.getEvalInstant()) &&
          JodaBeanUtils.equal(getComputeNodeId(), other.getComputeNodeId());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getId());
    hash += hash * 31 + JodaBeanUtils.hashCode(getCalculationConfigurationId());
    hash += hash * 31 + JodaBeanUtils.hashCode(getValueNameId());
    hash += hash * 31 + JodaBeanUtils.hashCode(getValueSpecificationId());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFunctionUniqueId());
    hash += hash * 31 + JodaBeanUtils.hashCode(getComputationTargetId());
    hash += hash * 31 + JodaBeanUtils.hashCode(getRunId());
    hash += hash * 31 + JodaBeanUtils.hashCode(getValue());
    hash += hash * 31 + JodaBeanUtils.hashCode(getEvalInstant());
    hash += hash * 31 + JodaBeanUtils.hashCode(getComputeNodeId());
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the id.
   * @return the value of the property
   */
  public long getId() {
    return _id;
  }

  /**
   * Sets the id.
   * @param id  the new value of the property
   */
  public void setId(long id) {
    this._id = id;
  }

  /**
   * Gets the the {@code id} property.
   * @return the property, not null
   */
  public final Property<Long> id() {
    return metaBean().id().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the calculationConfigurationId.
   * @return the value of the property
   */
  public long getCalculationConfigurationId() {
    return _calculationConfigurationId;
  }

  /**
   * Sets the calculationConfigurationId.
   * @param calculationConfigurationId  the new value of the property
   */
  public void setCalculationConfigurationId(long calculationConfigurationId) {
    this._calculationConfigurationId = calculationConfigurationId;
  }

  /**
   * Gets the the {@code calculationConfigurationId} property.
   * @return the property, not null
   */
  public final Property<Long> calculationConfigurationId() {
    return metaBean().calculationConfigurationId().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valueNameId.
   * @return the value of the property
   */
  public long getValueNameId() {
    return _valueNameId;
  }

  /**
   * Sets the valueNameId.
   * @param valueNameId  the new value of the property
   */
  public void setValueNameId(long valueNameId) {
    this._valueNameId = valueNameId;
  }

  /**
   * Gets the the {@code valueNameId} property.
   * @return the property, not null
   */
  public final Property<Long> valueNameId() {
    return metaBean().valueNameId().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valueSpecificationId.
   * @return the value of the property
   */
  public long getValueSpecificationId() {
    return _valueSpecificationId;
  }

  /**
   * Sets the valueSpecificationId.
   * @param valueSpecificationId  the new value of the property
   */
  public void setValueSpecificationId(long valueSpecificationId) {
    this._valueSpecificationId = valueSpecificationId;
  }

  /**
   * Gets the the {@code valueSpecificationId} property.
   * @return the property, not null
   */
  public final Property<Long> valueSpecificationId() {
    return metaBean().valueSpecificationId().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the functionUniqueId.
   * @return the value of the property
   */
  public long getFunctionUniqueId() {
    return _functionUniqueId;
  }

  /**
   * Sets the functionUniqueId.
   * @param functionUniqueId  the new value of the property
   */
  public void setFunctionUniqueId(long functionUniqueId) {
    this._functionUniqueId = functionUniqueId;
  }

  /**
   * Gets the the {@code functionUniqueId} property.
   * @return the property, not null
   */
  public final Property<Long> functionUniqueId() {
    return metaBean().functionUniqueId().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the computationTargetId.
   * @return the value of the property
   */
  public long getComputationTargetId() {
    return _computationTargetId;
  }

  /**
   * Sets the computationTargetId.
   * @param computationTargetId  the new value of the property
   */
  public void setComputationTargetId(long computationTargetId) {
    this._computationTargetId = computationTargetId;
  }

  /**
   * Gets the the {@code computationTargetId} property.
   * @return the property, not null
   */
  public final Property<Long> computationTargetId() {
    return metaBean().computationTargetId().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the runId.
   * @return the value of the property
   */
  public long getRunId() {
    return _runId;
  }

  /**
   * Sets the runId.
   * @param runId  the new value of the property
   */
  public void setRunId(long runId) {
    this._runId = runId;
  }

  /**
   * Gets the the {@code runId} property.
   * @return the property, not null
   */
  public final Property<Long> runId() {
    return metaBean().runId().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value.
   * @return the value of the property
   */
  public double getValue() {
    return _value;
  }

  /**
   * Sets the value.
   * @param value  the new value of the property
   */
  public void setValue(double value) {
    this._value = value;
  }

  /**
   * Gets the the {@code value} property.
   * @return the property, not null
   */
  public final Property<Double> value() {
    return metaBean().value().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the evalInstant.
   * @return the value of the property
   */
  public Instant getEvalInstant() {
    return _evalInstant;
  }

  /**
   * Sets the evalInstant.
   * @param evalInstant  the new value of the property
   */
  public void setEvalInstant(Instant evalInstant) {
    this._evalInstant = evalInstant;
  }

  /**
   * Gets the the {@code evalInstant} property.
   * @return the property, not null
   */
  public final Property<Instant> evalInstant() {
    return metaBean().evalInstant().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the computeNodeId.
   * @return the value of the property
   */
  public long getComputeNodeId() {
    return _computeNodeId;
  }

  /**
   * Sets the computeNodeId.
   * @param computeNodeId  the new value of the property
   */
  public void setComputeNodeId(long computeNodeId) {
    this._computeNodeId = computeNodeId;
  }

  /**
   * Gets the the {@code computeNodeId} property.
   * @return the property, not null
   */
  public final Property<Long> computeNodeId() {
    return metaBean().computeNodeId().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code RiskValue}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code id} property.
     */
    private final MetaProperty<Long> _id = DirectMetaProperty.ofReadWrite(
        this, "id", RiskValue.class, Long.TYPE);
    /**
     * The meta-property for the {@code calculationConfigurationId} property.
     */
    private final MetaProperty<Long> _calculationConfigurationId = DirectMetaProperty.ofReadWrite(
        this, "calculationConfigurationId", RiskValue.class, Long.TYPE);
    /**
     * The meta-property for the {@code valueNameId} property.
     */
    private final MetaProperty<Long> _valueNameId = DirectMetaProperty.ofReadWrite(
        this, "valueNameId", RiskValue.class, Long.TYPE);
    /**
     * The meta-property for the {@code valueSpecificationId} property.
     */
    private final MetaProperty<Long> _valueSpecificationId = DirectMetaProperty.ofReadWrite(
        this, "valueSpecificationId", RiskValue.class, Long.TYPE);
    /**
     * The meta-property for the {@code functionUniqueId} property.
     */
    private final MetaProperty<Long> _functionUniqueId = DirectMetaProperty.ofReadWrite(
        this, "functionUniqueId", RiskValue.class, Long.TYPE);
    /**
     * The meta-property for the {@code computationTargetId} property.
     */
    private final MetaProperty<Long> _computationTargetId = DirectMetaProperty.ofReadWrite(
        this, "computationTargetId", RiskValue.class, Long.TYPE);
    /**
     * The meta-property for the {@code runId} property.
     */
    private final MetaProperty<Long> _runId = DirectMetaProperty.ofReadWrite(
        this, "runId", RiskValue.class, Long.TYPE);
    /**
     * The meta-property for the {@code value} property.
     */
    private final MetaProperty<Double> _value = DirectMetaProperty.ofReadWrite(
        this, "value", RiskValue.class, Double.TYPE);
    /**
     * The meta-property for the {@code evalInstant} property.
     */
    private final MetaProperty<Instant> _evalInstant = DirectMetaProperty.ofReadWrite(
        this, "evalInstant", RiskValue.class, Instant.class);
    /**
     * The meta-property for the {@code computeNodeId} property.
     */
    private final MetaProperty<Long> _computeNodeId = DirectMetaProperty.ofReadWrite(
        this, "computeNodeId", RiskValue.class, Long.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<Object>> _map = new DirectMetaPropertyMap(
        this, null,
        "id",
        "calculationConfigurationId",
        "valueNameId",
        "valueSpecificationId",
        "functionUniqueId",
        "computationTargetId",
        "runId",
        "value",
        "evalInstant",
        "computeNodeId");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return _id;
        case 1329751720:  // calculationConfigurationId
          return _calculationConfigurationId;
        case -1585450537:  // valueNameId
          return _valueNameId;
        case -1127019571:  // valueSpecificationId
          return _valueSpecificationId;
        case 1563911364:  // functionUniqueId
          return _functionUniqueId;
        case -1362849421:  // computationTargetId
          return _computationTargetId;
        case 108875014:  // runId
          return _runId;
        case 111972721:  // value
          return _value;
        case 820536741:  // evalInstant
          return _evalInstant;
        case 398290388:  // computeNodeId
          return _computeNodeId;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends RiskValue> builder() {
      return new DirectBeanBuilder<RiskValue>(new RiskValue());
    }

    @Override
    public Class<? extends RiskValue> beanType() {
      return RiskValue.class;
    }

    @Override
    public Map<String, MetaProperty<Object>> metaPropertyMap() {
      return _map;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code id} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Long> id() {
      return _id;
    }

    /**
     * The meta-property for the {@code calculationConfigurationId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Long> calculationConfigurationId() {
      return _calculationConfigurationId;
    }

    /**
     * The meta-property for the {@code valueNameId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Long> valueNameId() {
      return _valueNameId;
    }

    /**
     * The meta-property for the {@code valueSpecificationId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Long> valueSpecificationId() {
      return _valueSpecificationId;
    }

    /**
     * The meta-property for the {@code functionUniqueId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Long> functionUniqueId() {
      return _functionUniqueId;
    }

    /**
     * The meta-property for the {@code computationTargetId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Long> computationTargetId() {
      return _computationTargetId;
    }

    /**
     * The meta-property for the {@code runId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Long> runId() {
      return _runId;
    }

    /**
     * The meta-property for the {@code value} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Double> value() {
      return _value;
    }

    /**
     * The meta-property for the {@code evalInstant} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Instant> evalInstant() {
      return _evalInstant;
    }

    /**
     * The meta-property for the {@code computeNodeId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Long> computeNodeId() {
      return _computeNodeId;
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
