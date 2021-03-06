/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.commerce.price.list.model.impl;

import aQute.bnd.annotation.ProviderType;

import com.liferay.commerce.price.list.model.CommerceTierPriceEntry;
import com.liferay.commerce.price.list.model.CommerceTierPriceEntryModel;
import com.liferay.commerce.price.list.model.CommerceTierPriceEntrySoap;

import com.liferay.expando.kernel.model.ExpandoBridge;
import com.liferay.expando.kernel.util.ExpandoBridgeFactoryUtil;

import com.liferay.exportimport.kernel.lar.StagedModelType;

import com.liferay.portal.kernel.bean.AutoEscapeBeanHandler;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSON;
import com.liferay.portal.kernel.model.CacheModel;
import com.liferay.portal.kernel.model.ModelWrapper;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.impl.BaseModelImpl;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.kernel.util.StringBundler;

import java.io.Serializable;

import java.math.BigDecimal;

import java.sql.Types;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The base model implementation for the CommerceTierPriceEntry service. Represents a row in the &quot;CommerceTierPriceEntry&quot; database table, with each column mapped to a property of this class.
 *
 * <p>
 * This implementation and its corresponding interface {@link CommerceTierPriceEntryModel} exist only as a container for the default property accessors generated by ServiceBuilder. Helper methods and all application logic should be put in {@link CommerceTierPriceEntryImpl}.
 * </p>
 *
 * @author Alessio Antonio Rendina
 * @see CommerceTierPriceEntryImpl
 * @see CommerceTierPriceEntry
 * @see CommerceTierPriceEntryModel
 * @generated
 */
@JSON(strict = true)
@ProviderType
public class CommerceTierPriceEntryModelImpl extends BaseModelImpl<CommerceTierPriceEntry>
	implements CommerceTierPriceEntryModel {
	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this class directly. All methods that expect a commerce tier price entry model instance should use the {@link CommerceTierPriceEntry} interface instead.
	 */
	public static final String TABLE_NAME = "CommerceTierPriceEntry";
	public static final Object[][] TABLE_COLUMNS = {
			{ "uuid_", Types.VARCHAR },
			{ "commerceTierPriceEntryId", Types.BIGINT },
			{ "groupId", Types.BIGINT },
			{ "companyId", Types.BIGINT },
			{ "userId", Types.BIGINT },
			{ "userName", Types.VARCHAR },
			{ "createDate", Types.TIMESTAMP },
			{ "modifiedDate", Types.TIMESTAMP },
			{ "commercePriceEntryId", Types.BIGINT },
			{ "externalReferenceCode", Types.VARCHAR },
			{ "price", Types.DECIMAL },
			{ "promoPrice", Types.DECIMAL },
			{ "minQuantity", Types.INTEGER },
			{ "lastPublishDate", Types.TIMESTAMP }
		};
	public static final Map<String, Integer> TABLE_COLUMNS_MAP = new HashMap<String, Integer>();

	static {
		TABLE_COLUMNS_MAP.put("uuid_", Types.VARCHAR);
		TABLE_COLUMNS_MAP.put("commerceTierPriceEntryId", Types.BIGINT);
		TABLE_COLUMNS_MAP.put("groupId", Types.BIGINT);
		TABLE_COLUMNS_MAP.put("companyId", Types.BIGINT);
		TABLE_COLUMNS_MAP.put("userId", Types.BIGINT);
		TABLE_COLUMNS_MAP.put("userName", Types.VARCHAR);
		TABLE_COLUMNS_MAP.put("createDate", Types.TIMESTAMP);
		TABLE_COLUMNS_MAP.put("modifiedDate", Types.TIMESTAMP);
		TABLE_COLUMNS_MAP.put("commercePriceEntryId", Types.BIGINT);
		TABLE_COLUMNS_MAP.put("externalReferenceCode", Types.VARCHAR);
		TABLE_COLUMNS_MAP.put("price", Types.DECIMAL);
		TABLE_COLUMNS_MAP.put("promoPrice", Types.DECIMAL);
		TABLE_COLUMNS_MAP.put("minQuantity", Types.INTEGER);
		TABLE_COLUMNS_MAP.put("lastPublishDate", Types.TIMESTAMP);
	}

	public static final String TABLE_SQL_CREATE = "create table CommerceTierPriceEntry (uuid_ VARCHAR(75) null,commerceTierPriceEntryId LONG not null primary key,groupId LONG,companyId LONG,userId LONG,userName VARCHAR(75) null,createDate DATE null,modifiedDate DATE null,commercePriceEntryId LONG,externalReferenceCode VARCHAR(75) null,price DECIMAL(30, 16) null,promoPrice DECIMAL(30, 16) null,minQuantity INTEGER,lastPublishDate DATE null)";
	public static final String TABLE_SQL_DROP = "drop table CommerceTierPriceEntry";
	public static final String ORDER_BY_JPQL = " ORDER BY commerceTierPriceEntry.minQuantity ASC";
	public static final String ORDER_BY_SQL = " ORDER BY CommerceTierPriceEntry.minQuantity ASC";
	public static final String DATA_SOURCE = "liferayDataSource";
	public static final String SESSION_FACTORY = "liferaySessionFactory";
	public static final String TX_MANAGER = "liferayTransactionManager";
	public static final boolean ENTITY_CACHE_ENABLED = GetterUtil.getBoolean(com.liferay.commerce.price.list.service.util.ServiceProps.get(
				"value.object.entity.cache.enabled.com.liferay.commerce.price.list.model.CommerceTierPriceEntry"),
			true);
	public static final boolean FINDER_CACHE_ENABLED = GetterUtil.getBoolean(com.liferay.commerce.price.list.service.util.ServiceProps.get(
				"value.object.finder.cache.enabled.com.liferay.commerce.price.list.model.CommerceTierPriceEntry"),
			true);
	public static final boolean COLUMN_BITMASK_ENABLED = GetterUtil.getBoolean(com.liferay.commerce.price.list.service.util.ServiceProps.get(
				"value.object.column.bitmask.enabled.com.liferay.commerce.price.list.model.CommerceTierPriceEntry"),
			true);
	public static final long COMMERCEPRICEENTRYID_COLUMN_BITMASK = 1L;
	public static final long COMPANYID_COLUMN_BITMASK = 2L;
	public static final long EXTERNALREFERENCECODE_COLUMN_BITMASK = 4L;
	public static final long GROUPID_COLUMN_BITMASK = 8L;
	public static final long MINQUANTITY_COLUMN_BITMASK = 16L;
	public static final long UUID_COLUMN_BITMASK = 32L;

	/**
	 * Converts the soap model instance into a normal model instance.
	 *
	 * @param soapModel the soap model instance to convert
	 * @return the normal model instance
	 */
	public static CommerceTierPriceEntry toModel(
		CommerceTierPriceEntrySoap soapModel) {
		if (soapModel == null) {
			return null;
		}

		CommerceTierPriceEntry model = new CommerceTierPriceEntryImpl();

		model.setUuid(soapModel.getUuid());
		model.setCommerceTierPriceEntryId(soapModel.getCommerceTierPriceEntryId());
		model.setGroupId(soapModel.getGroupId());
		model.setCompanyId(soapModel.getCompanyId());
		model.setUserId(soapModel.getUserId());
		model.setUserName(soapModel.getUserName());
		model.setCreateDate(soapModel.getCreateDate());
		model.setModifiedDate(soapModel.getModifiedDate());
		model.setCommercePriceEntryId(soapModel.getCommercePriceEntryId());
		model.setExternalReferenceCode(soapModel.getExternalReferenceCode());
		model.setPrice(soapModel.getPrice());
		model.setPromoPrice(soapModel.getPromoPrice());
		model.setMinQuantity(soapModel.getMinQuantity());
		model.setLastPublishDate(soapModel.getLastPublishDate());

		return model;
	}

	/**
	 * Converts the soap model instances into normal model instances.
	 *
	 * @param soapModels the soap model instances to convert
	 * @return the normal model instances
	 */
	public static List<CommerceTierPriceEntry> toModels(
		CommerceTierPriceEntrySoap[] soapModels) {
		if (soapModels == null) {
			return null;
		}

		List<CommerceTierPriceEntry> models = new ArrayList<CommerceTierPriceEntry>(soapModels.length);

		for (CommerceTierPriceEntrySoap soapModel : soapModels) {
			models.add(toModel(soapModel));
		}

		return models;
	}

	public static final long LOCK_EXPIRATION_TIME = GetterUtil.getLong(com.liferay.commerce.price.list.service.util.ServiceProps.get(
				"lock.expiration.time.com.liferay.commerce.price.list.model.CommerceTierPriceEntry"));

	public CommerceTierPriceEntryModelImpl() {
	}

	@Override
	public long getPrimaryKey() {
		return _commerceTierPriceEntryId;
	}

	@Override
	public void setPrimaryKey(long primaryKey) {
		setCommerceTierPriceEntryId(primaryKey);
	}

	@Override
	public Serializable getPrimaryKeyObj() {
		return _commerceTierPriceEntryId;
	}

	@Override
	public void setPrimaryKeyObj(Serializable primaryKeyObj) {
		setPrimaryKey(((Long)primaryKeyObj).longValue());
	}

	@Override
	public Class<?> getModelClass() {
		return CommerceTierPriceEntry.class;
	}

	@Override
	public String getModelClassName() {
		return CommerceTierPriceEntry.class.getName();
	}

	@Override
	public Map<String, Object> getModelAttributes() {
		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put("uuid", getUuid());
		attributes.put("commerceTierPriceEntryId", getCommerceTierPriceEntryId());
		attributes.put("groupId", getGroupId());
		attributes.put("companyId", getCompanyId());
		attributes.put("userId", getUserId());
		attributes.put("userName", getUserName());
		attributes.put("createDate", getCreateDate());
		attributes.put("modifiedDate", getModifiedDate());
		attributes.put("commercePriceEntryId", getCommercePriceEntryId());
		attributes.put("externalReferenceCode", getExternalReferenceCode());
		attributes.put("price", getPrice());
		attributes.put("promoPrice", getPromoPrice());
		attributes.put("minQuantity", getMinQuantity());
		attributes.put("lastPublishDate", getLastPublishDate());

		attributes.put("entityCacheEnabled", isEntityCacheEnabled());
		attributes.put("finderCacheEnabled", isFinderCacheEnabled());

		return attributes;
	}

	@Override
	public void setModelAttributes(Map<String, Object> attributes) {
		String uuid = (String)attributes.get("uuid");

		if (uuid != null) {
			setUuid(uuid);
		}

		Long commerceTierPriceEntryId = (Long)attributes.get(
				"commerceTierPriceEntryId");

		if (commerceTierPriceEntryId != null) {
			setCommerceTierPriceEntryId(commerceTierPriceEntryId);
		}

		Long groupId = (Long)attributes.get("groupId");

		if (groupId != null) {
			setGroupId(groupId);
		}

		Long companyId = (Long)attributes.get("companyId");

		if (companyId != null) {
			setCompanyId(companyId);
		}

		Long userId = (Long)attributes.get("userId");

		if (userId != null) {
			setUserId(userId);
		}

		String userName = (String)attributes.get("userName");

		if (userName != null) {
			setUserName(userName);
		}

		Date createDate = (Date)attributes.get("createDate");

		if (createDate != null) {
			setCreateDate(createDate);
		}

		Date modifiedDate = (Date)attributes.get("modifiedDate");

		if (modifiedDate != null) {
			setModifiedDate(modifiedDate);
		}

		Long commercePriceEntryId = (Long)attributes.get("commercePriceEntryId");

		if (commercePriceEntryId != null) {
			setCommercePriceEntryId(commercePriceEntryId);
		}

		String externalReferenceCode = (String)attributes.get(
				"externalReferenceCode");

		if (externalReferenceCode != null) {
			setExternalReferenceCode(externalReferenceCode);
		}

		BigDecimal price = (BigDecimal)attributes.get("price");

		if (price != null) {
			setPrice(price);
		}

		BigDecimal promoPrice = (BigDecimal)attributes.get("promoPrice");

		if (promoPrice != null) {
			setPromoPrice(promoPrice);
		}

		Integer minQuantity = (Integer)attributes.get("minQuantity");

		if (minQuantity != null) {
			setMinQuantity(minQuantity);
		}

		Date lastPublishDate = (Date)attributes.get("lastPublishDate");

		if (lastPublishDate != null) {
			setLastPublishDate(lastPublishDate);
		}
	}

	@JSON
	@Override
	public String getUuid() {
		if (_uuid == null) {
			return "";
		}
		else {
			return _uuid;
		}
	}

	@Override
	public void setUuid(String uuid) {
		if (_originalUuid == null) {
			_originalUuid = _uuid;
		}

		_uuid = uuid;
	}

	public String getOriginalUuid() {
		return GetterUtil.getString(_originalUuid);
	}

	@JSON
	@Override
	public long getCommerceTierPriceEntryId() {
		return _commerceTierPriceEntryId;
	}

	@Override
	public void setCommerceTierPriceEntryId(long commerceTierPriceEntryId) {
		_commerceTierPriceEntryId = commerceTierPriceEntryId;
	}

	@JSON
	@Override
	public long getGroupId() {
		return _groupId;
	}

	@Override
	public void setGroupId(long groupId) {
		_columnBitmask |= GROUPID_COLUMN_BITMASK;

		if (!_setOriginalGroupId) {
			_setOriginalGroupId = true;

			_originalGroupId = _groupId;
		}

		_groupId = groupId;
	}

	public long getOriginalGroupId() {
		return _originalGroupId;
	}

	@JSON
	@Override
	public long getCompanyId() {
		return _companyId;
	}

	@Override
	public void setCompanyId(long companyId) {
		_columnBitmask |= COMPANYID_COLUMN_BITMASK;

		if (!_setOriginalCompanyId) {
			_setOriginalCompanyId = true;

			_originalCompanyId = _companyId;
		}

		_companyId = companyId;
	}

	public long getOriginalCompanyId() {
		return _originalCompanyId;
	}

	@JSON
	@Override
	public long getUserId() {
		return _userId;
	}

	@Override
	public void setUserId(long userId) {
		_userId = userId;
	}

	@Override
	public String getUserUuid() {
		try {
			User user = UserLocalServiceUtil.getUserById(getUserId());

			return user.getUuid();
		}
		catch (PortalException pe) {
			return "";
		}
	}

	@Override
	public void setUserUuid(String userUuid) {
	}

	@JSON
	@Override
	public String getUserName() {
		if (_userName == null) {
			return "";
		}
		else {
			return _userName;
		}
	}

	@Override
	public void setUserName(String userName) {
		_userName = userName;
	}

	@JSON
	@Override
	public Date getCreateDate() {
		return _createDate;
	}

	@Override
	public void setCreateDate(Date createDate) {
		_createDate = createDate;
	}

	@JSON
	@Override
	public Date getModifiedDate() {
		return _modifiedDate;
	}

	public boolean hasSetModifiedDate() {
		return _setModifiedDate;
	}

	@Override
	public void setModifiedDate(Date modifiedDate) {
		_setModifiedDate = true;

		_modifiedDate = modifiedDate;
	}

	@JSON
	@Override
	public long getCommercePriceEntryId() {
		return _commercePriceEntryId;
	}

	@Override
	public void setCommercePriceEntryId(long commercePriceEntryId) {
		_columnBitmask |= COMMERCEPRICEENTRYID_COLUMN_BITMASK;

		if (!_setOriginalCommercePriceEntryId) {
			_setOriginalCommercePriceEntryId = true;

			_originalCommercePriceEntryId = _commercePriceEntryId;
		}

		_commercePriceEntryId = commercePriceEntryId;
	}

	public long getOriginalCommercePriceEntryId() {
		return _originalCommercePriceEntryId;
	}

	@JSON
	@Override
	public String getExternalReferenceCode() {
		if (_externalReferenceCode == null) {
			return "";
		}
		else {
			return _externalReferenceCode;
		}
	}

	@Override
	public void setExternalReferenceCode(String externalReferenceCode) {
		_columnBitmask |= EXTERNALREFERENCECODE_COLUMN_BITMASK;

		if (_originalExternalReferenceCode == null) {
			_originalExternalReferenceCode = _externalReferenceCode;
		}

		_externalReferenceCode = externalReferenceCode;
	}

	public String getOriginalExternalReferenceCode() {
		return GetterUtil.getString(_originalExternalReferenceCode);
	}

	@JSON
	@Override
	public BigDecimal getPrice() {
		return _price;
	}

	@Override
	public void setPrice(BigDecimal price) {
		_price = price;
	}

	@JSON
	@Override
	public BigDecimal getPromoPrice() {
		return _promoPrice;
	}

	@Override
	public void setPromoPrice(BigDecimal promoPrice) {
		_promoPrice = promoPrice;
	}

	@JSON
	@Override
	public int getMinQuantity() {
		return _minQuantity;
	}

	@Override
	public void setMinQuantity(int minQuantity) {
		_columnBitmask = -1L;

		if (!_setOriginalMinQuantity) {
			_setOriginalMinQuantity = true;

			_originalMinQuantity = _minQuantity;
		}

		_minQuantity = minQuantity;
	}

	public int getOriginalMinQuantity() {
		return _originalMinQuantity;
	}

	@JSON
	@Override
	public Date getLastPublishDate() {
		return _lastPublishDate;
	}

	@Override
	public void setLastPublishDate(Date lastPublishDate) {
		_lastPublishDate = lastPublishDate;
	}

	@Override
	public StagedModelType getStagedModelType() {
		return new StagedModelType(PortalUtil.getClassNameId(
				CommerceTierPriceEntry.class.getName()));
	}

	public long getColumnBitmask() {
		return _columnBitmask;
	}

	@Override
	public ExpandoBridge getExpandoBridge() {
		return ExpandoBridgeFactoryUtil.getExpandoBridge(getCompanyId(),
			CommerceTierPriceEntry.class.getName(), getPrimaryKey());
	}

	@Override
	public void setExpandoBridgeAttributes(ServiceContext serviceContext) {
		ExpandoBridge expandoBridge = getExpandoBridge();

		expandoBridge.setAttributes(serviceContext);
	}

	@Override
	public CommerceTierPriceEntry toEscapedModel() {
		if (_escapedModel == null) {
			_escapedModel = (CommerceTierPriceEntry)ProxyUtil.newProxyInstance(_classLoader,
					_escapedModelInterfaces, new AutoEscapeBeanHandler(this));
		}

		return _escapedModel;
	}

	@Override
	public Object clone() {
		CommerceTierPriceEntryImpl commerceTierPriceEntryImpl = new CommerceTierPriceEntryImpl();

		commerceTierPriceEntryImpl.setUuid(getUuid());
		commerceTierPriceEntryImpl.setCommerceTierPriceEntryId(getCommerceTierPriceEntryId());
		commerceTierPriceEntryImpl.setGroupId(getGroupId());
		commerceTierPriceEntryImpl.setCompanyId(getCompanyId());
		commerceTierPriceEntryImpl.setUserId(getUserId());
		commerceTierPriceEntryImpl.setUserName(getUserName());
		commerceTierPriceEntryImpl.setCreateDate(getCreateDate());
		commerceTierPriceEntryImpl.setModifiedDate(getModifiedDate());
		commerceTierPriceEntryImpl.setCommercePriceEntryId(getCommercePriceEntryId());
		commerceTierPriceEntryImpl.setExternalReferenceCode(getExternalReferenceCode());
		commerceTierPriceEntryImpl.setPrice(getPrice());
		commerceTierPriceEntryImpl.setPromoPrice(getPromoPrice());
		commerceTierPriceEntryImpl.setMinQuantity(getMinQuantity());
		commerceTierPriceEntryImpl.setLastPublishDate(getLastPublishDate());

		commerceTierPriceEntryImpl.resetOriginalValues();

		return commerceTierPriceEntryImpl;
	}

	@Override
	public int compareTo(CommerceTierPriceEntry commerceTierPriceEntry) {
		int value = 0;

		if (getMinQuantity() < commerceTierPriceEntry.getMinQuantity()) {
			value = -1;
		}
		else if (getMinQuantity() > commerceTierPriceEntry.getMinQuantity()) {
			value = 1;
		}
		else {
			value = 0;
		}

		if (value != 0) {
			return value;
		}

		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof CommerceTierPriceEntry)) {
			return false;
		}

		CommerceTierPriceEntry commerceTierPriceEntry = (CommerceTierPriceEntry)obj;

		long primaryKey = commerceTierPriceEntry.getPrimaryKey();

		if (getPrimaryKey() == primaryKey) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return (int)getPrimaryKey();
	}

	@Override
	public boolean isEntityCacheEnabled() {
		return ENTITY_CACHE_ENABLED;
	}

	@Override
	public boolean isFinderCacheEnabled() {
		return FINDER_CACHE_ENABLED;
	}

	@Override
	public void resetOriginalValues() {
		CommerceTierPriceEntryModelImpl commerceTierPriceEntryModelImpl = this;

		commerceTierPriceEntryModelImpl._originalUuid = commerceTierPriceEntryModelImpl._uuid;

		commerceTierPriceEntryModelImpl._originalGroupId = commerceTierPriceEntryModelImpl._groupId;

		commerceTierPriceEntryModelImpl._setOriginalGroupId = false;

		commerceTierPriceEntryModelImpl._originalCompanyId = commerceTierPriceEntryModelImpl._companyId;

		commerceTierPriceEntryModelImpl._setOriginalCompanyId = false;

		commerceTierPriceEntryModelImpl._setModifiedDate = false;

		commerceTierPriceEntryModelImpl._originalCommercePriceEntryId = commerceTierPriceEntryModelImpl._commercePriceEntryId;

		commerceTierPriceEntryModelImpl._setOriginalCommercePriceEntryId = false;

		commerceTierPriceEntryModelImpl._originalExternalReferenceCode = commerceTierPriceEntryModelImpl._externalReferenceCode;

		commerceTierPriceEntryModelImpl._originalMinQuantity = commerceTierPriceEntryModelImpl._minQuantity;

		commerceTierPriceEntryModelImpl._setOriginalMinQuantity = false;

		commerceTierPriceEntryModelImpl._columnBitmask = 0;
	}

	@Override
	public CacheModel<CommerceTierPriceEntry> toCacheModel() {
		CommerceTierPriceEntryCacheModel commerceTierPriceEntryCacheModel = new CommerceTierPriceEntryCacheModel();

		commerceTierPriceEntryCacheModel.uuid = getUuid();

		String uuid = commerceTierPriceEntryCacheModel.uuid;

		if ((uuid != null) && (uuid.length() == 0)) {
			commerceTierPriceEntryCacheModel.uuid = null;
		}

		commerceTierPriceEntryCacheModel.commerceTierPriceEntryId = getCommerceTierPriceEntryId();

		commerceTierPriceEntryCacheModel.groupId = getGroupId();

		commerceTierPriceEntryCacheModel.companyId = getCompanyId();

		commerceTierPriceEntryCacheModel.userId = getUserId();

		commerceTierPriceEntryCacheModel.userName = getUserName();

		String userName = commerceTierPriceEntryCacheModel.userName;

		if ((userName != null) && (userName.length() == 0)) {
			commerceTierPriceEntryCacheModel.userName = null;
		}

		Date createDate = getCreateDate();

		if (createDate != null) {
			commerceTierPriceEntryCacheModel.createDate = createDate.getTime();
		}
		else {
			commerceTierPriceEntryCacheModel.createDate = Long.MIN_VALUE;
		}

		Date modifiedDate = getModifiedDate();

		if (modifiedDate != null) {
			commerceTierPriceEntryCacheModel.modifiedDate = modifiedDate.getTime();
		}
		else {
			commerceTierPriceEntryCacheModel.modifiedDate = Long.MIN_VALUE;
		}

		commerceTierPriceEntryCacheModel.commercePriceEntryId = getCommercePriceEntryId();

		commerceTierPriceEntryCacheModel.externalReferenceCode = getExternalReferenceCode();

		String externalReferenceCode = commerceTierPriceEntryCacheModel.externalReferenceCode;

		if ((externalReferenceCode != null) &&
				(externalReferenceCode.length() == 0)) {
			commerceTierPriceEntryCacheModel.externalReferenceCode = null;
		}

		commerceTierPriceEntryCacheModel.price = getPrice();

		commerceTierPriceEntryCacheModel.promoPrice = getPromoPrice();

		commerceTierPriceEntryCacheModel.minQuantity = getMinQuantity();

		Date lastPublishDate = getLastPublishDate();

		if (lastPublishDate != null) {
			commerceTierPriceEntryCacheModel.lastPublishDate = lastPublishDate.getTime();
		}
		else {
			commerceTierPriceEntryCacheModel.lastPublishDate = Long.MIN_VALUE;
		}

		return commerceTierPriceEntryCacheModel;
	}

	@Override
	public String toString() {
		StringBundler sb = new StringBundler(29);

		sb.append("{uuid=");
		sb.append(getUuid());
		sb.append(", commerceTierPriceEntryId=");
		sb.append(getCommerceTierPriceEntryId());
		sb.append(", groupId=");
		sb.append(getGroupId());
		sb.append(", companyId=");
		sb.append(getCompanyId());
		sb.append(", userId=");
		sb.append(getUserId());
		sb.append(", userName=");
		sb.append(getUserName());
		sb.append(", createDate=");
		sb.append(getCreateDate());
		sb.append(", modifiedDate=");
		sb.append(getModifiedDate());
		sb.append(", commercePriceEntryId=");
		sb.append(getCommercePriceEntryId());
		sb.append(", externalReferenceCode=");
		sb.append(getExternalReferenceCode());
		sb.append(", price=");
		sb.append(getPrice());
		sb.append(", promoPrice=");
		sb.append(getPromoPrice());
		sb.append(", minQuantity=");
		sb.append(getMinQuantity());
		sb.append(", lastPublishDate=");
		sb.append(getLastPublishDate());
		sb.append("}");

		return sb.toString();
	}

	@Override
	public String toXmlString() {
		StringBundler sb = new StringBundler(46);

		sb.append("<model><model-name>");
		sb.append(
			"com.liferay.commerce.price.list.model.CommerceTierPriceEntry");
		sb.append("</model-name>");

		sb.append(
			"<column><column-name>uuid</column-name><column-value><![CDATA[");
		sb.append(getUuid());
		sb.append("]]></column-value></column>");
		sb.append(
			"<column><column-name>commerceTierPriceEntryId</column-name><column-value><![CDATA[");
		sb.append(getCommerceTierPriceEntryId());
		sb.append("]]></column-value></column>");
		sb.append(
			"<column><column-name>groupId</column-name><column-value><![CDATA[");
		sb.append(getGroupId());
		sb.append("]]></column-value></column>");
		sb.append(
			"<column><column-name>companyId</column-name><column-value><![CDATA[");
		sb.append(getCompanyId());
		sb.append("]]></column-value></column>");
		sb.append(
			"<column><column-name>userId</column-name><column-value><![CDATA[");
		sb.append(getUserId());
		sb.append("]]></column-value></column>");
		sb.append(
			"<column><column-name>userName</column-name><column-value><![CDATA[");
		sb.append(getUserName());
		sb.append("]]></column-value></column>");
		sb.append(
			"<column><column-name>createDate</column-name><column-value><![CDATA[");
		sb.append(getCreateDate());
		sb.append("]]></column-value></column>");
		sb.append(
			"<column><column-name>modifiedDate</column-name><column-value><![CDATA[");
		sb.append(getModifiedDate());
		sb.append("]]></column-value></column>");
		sb.append(
			"<column><column-name>commercePriceEntryId</column-name><column-value><![CDATA[");
		sb.append(getCommercePriceEntryId());
		sb.append("]]></column-value></column>");
		sb.append(
			"<column><column-name>externalReferenceCode</column-name><column-value><![CDATA[");
		sb.append(getExternalReferenceCode());
		sb.append("]]></column-value></column>");
		sb.append(
			"<column><column-name>price</column-name><column-value><![CDATA[");
		sb.append(getPrice());
		sb.append("]]></column-value></column>");
		sb.append(
			"<column><column-name>promoPrice</column-name><column-value><![CDATA[");
		sb.append(getPromoPrice());
		sb.append("]]></column-value></column>");
		sb.append(
			"<column><column-name>minQuantity</column-name><column-value><![CDATA[");
		sb.append(getMinQuantity());
		sb.append("]]></column-value></column>");
		sb.append(
			"<column><column-name>lastPublishDate</column-name><column-value><![CDATA[");
		sb.append(getLastPublishDate());
		sb.append("]]></column-value></column>");

		sb.append("</model>");

		return sb.toString();
	}

	private static final ClassLoader _classLoader = CommerceTierPriceEntry.class.getClassLoader();
	private static final Class<?>[] _escapedModelInterfaces = new Class[] {
			CommerceTierPriceEntry.class, ModelWrapper.class
		};
	private String _uuid;
	private String _originalUuid;
	private long _commerceTierPriceEntryId;
	private long _groupId;
	private long _originalGroupId;
	private boolean _setOriginalGroupId;
	private long _companyId;
	private long _originalCompanyId;
	private boolean _setOriginalCompanyId;
	private long _userId;
	private String _userName;
	private Date _createDate;
	private Date _modifiedDate;
	private boolean _setModifiedDate;
	private long _commercePriceEntryId;
	private long _originalCommercePriceEntryId;
	private boolean _setOriginalCommercePriceEntryId;
	private String _externalReferenceCode;
	private String _originalExternalReferenceCode;
	private BigDecimal _price;
	private BigDecimal _promoPrice;
	private int _minQuantity;
	private int _originalMinQuantity;
	private boolean _setOriginalMinQuantity;
	private Date _lastPublishDate;
	private long _columnBitmask;
	private CommerceTierPriceEntry _escapedModel;
}