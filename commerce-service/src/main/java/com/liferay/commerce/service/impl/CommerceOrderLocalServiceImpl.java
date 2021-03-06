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

package com.liferay.commerce.service.impl;

import com.liferay.commerce.configuration.CommerceOrderConfiguration;
import com.liferay.commerce.constants.CommerceConstants;
import com.liferay.commerce.constants.CommerceDestinationNames;
import com.liferay.commerce.constants.CommerceOrderConstants;
import com.liferay.commerce.constants.CommerceOrderPaymentConstants;
import com.liferay.commerce.context.CommerceContext;
import com.liferay.commerce.currency.model.CommerceCurrency;
import com.liferay.commerce.currency.model.CommerceMoney;
import com.liferay.commerce.currency.service.CommerceCurrencyLocalService;
import com.liferay.commerce.discount.CommerceDiscountValue;
import com.liferay.commerce.exception.CommerceOrderBillingAddressException;
import com.liferay.commerce.exception.CommerceOrderPaymentMethodException;
import com.liferay.commerce.exception.CommerceOrderPurchaseOrderNumberException;
import com.liferay.commerce.exception.CommerceOrderShippingAddressException;
import com.liferay.commerce.exception.CommerceOrderShippingMethodException;
import com.liferay.commerce.exception.CommerceOrderStatusException;
import com.liferay.commerce.exception.CommercePaymentEngineException;
import com.liferay.commerce.exception.GuestCartMaxAllowedException;
import com.liferay.commerce.exception.NoSuchPaymentMethodException;
import com.liferay.commerce.model.CommerceAddress;
import com.liferay.commerce.model.CommerceOrder;
import com.liferay.commerce.model.CommerceOrderItem;
import com.liferay.commerce.model.CommercePaymentEngine;
import com.liferay.commerce.model.CommercePaymentEngineResult;
import com.liferay.commerce.model.CommercePaymentMethod;
import com.liferay.commerce.model.CommerceShippingMethod;
import com.liferay.commerce.organization.service.CommerceOrganizationLocalService;
import com.liferay.commerce.price.CommerceOrderPrice;
import com.liferay.commerce.price.CommerceOrderPriceCalculation;
import com.liferay.commerce.product.util.DDMFormValuesHelper;
import com.liferay.commerce.service.base.CommerceOrderLocalServiceBaseImpl;
import com.liferay.commerce.util.CommercePaymentEngineRegistry;
import com.liferay.commerce.util.CommerceShippingHelper;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.UserConstants;
import com.liferay.portal.kernel.search.BaseModelSearchResult;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.Indexable;
import com.liferay.portal.kernel.search.IndexableType;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.transaction.TransactionCommitCallbackUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StackTraceUtil;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.URLCodec;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.workflow.WorkflowHandlerRegistryUtil;
import com.liferay.portal.kernel.workflow.WorkflowTask;
import com.liferay.portal.kernel.workflow.WorkflowTaskManager;
import com.liferay.portal.spring.extender.service.ServiceReference;

import java.io.Serializable;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author Andrea Di Giorgi
 * @author Alessio Antonio Rendina
 * @author Marco Leo
 */
public class CommerceOrderLocalServiceImpl
	extends CommerceOrderLocalServiceBaseImpl {

	@Override
	public CommerceOrder addCommerceOrder(
			long siteGroupId, long orderOrganizationId, long orderUserId,
			long commerceCurrencyId, long billingAddressId,
			long shippingAddressId, long commercePaymentMethodId,
			long commerceShippingMethodId, String shippingOptionName,
			String purchaseOrderNumber, BigDecimal subtotal,
			BigDecimal shippingAmount, BigDecimal total, int paymentStatus,
			int orderStatus, ServiceContext serviceContext)
		throws PortalException {

		// Commerce order

		long scopeGroupId = serviceContext.getScopeGroupId();
		long userId = serviceContext.getUserId();

		User user = userLocalService.getUser(userId);

		if (user.isDefaultUser()) {
			userId = 0;
		}

		validateGuestOrders();

		if (commerceCurrencyId <= 0) {
			CommerceCurrency commerceCurrency =
				_commerceCurrencyLocalService.fetchPrimaryCommerceCurrency(
					siteGroupId);

			if (commerceCurrency != null) {
				commerceCurrencyId = commerceCurrency.getCommerceCurrencyId();
			}
		}

		long commerceOrderId = counterLocalService.increment();

		CommerceOrder commerceOrder = commerceOrderPersistence.create(
			commerceOrderId);

		commerceOrder.setUuid(serviceContext.getUuid());
		commerceOrder.setGroupId(scopeGroupId);
		commerceOrder.setCompanyId(user.getCompanyId());
		commerceOrder.setUserId(userId);
		commerceOrder.setUserName(user.getFullName());
		commerceOrder.setSiteGroupId(siteGroupId);
		commerceOrder.setOrderOrganizationId(orderOrganizationId);
		commerceOrder.setOrderUserId(orderUserId);
		commerceOrder.setCommerceCurrencyId(commerceCurrencyId);
		commerceOrder.setBillingAddressId(billingAddressId);
		commerceOrder.setShippingAddressId(shippingAddressId);
		commerceOrder.setCommercePaymentMethodId(commercePaymentMethodId);
		commerceOrder.setCommerceShippingMethodId(commerceShippingMethodId);
		commerceOrder.setShippingOptionName(shippingOptionName);
		commerceOrder.setPurchaseOrderNumber(purchaseOrderNumber);
		commerceOrder.setSubtotal(subtotal);
		commerceOrder.setShippingAmount(shippingAmount);
		commerceOrder.setTotal(total);
		commerceOrder.setPaymentStatus(paymentStatus);
		commerceOrder.setOrderStatus(orderStatus);
		commerceOrder.setStatus(WorkflowConstants.STATUS_DRAFT);
		commerceOrder.setStatusByUserId(user.getUserId());
		commerceOrder.setStatusByUserName(user.getFullName());
		commerceOrder.setStatusDate(serviceContext.getModifiedDate(null));
		commerceOrder.setExpandoBridgeAttributes(serviceContext);

		commerceOrderPersistence.update(commerceOrder);

		// Workflow

		return startWorkflowInstance(
			user.getUserId(), commerceOrder, serviceContext);
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceOrder addOrganizationCommerceOrder(
			long groupId, long userId, long siteGroupId,
			long orderOrganizationId, long shippingAddressId,
			String purchaseOrderNumber)
		throws PortalException {

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(groupId);
		serviceContext.setUserId(userId);

		if (hasWorkflowDefinition(
				siteGroupId, CommerceOrderConstants.TYPE_PK_APPROVAL)) {

			serviceContext.setWorkflowAction(
				WorkflowConstants.ACTION_SAVE_DRAFT);
		}

		return addCommerceOrder(
			siteGroupId, orderOrganizationId, userId, 0, 0, shippingAddressId,
			0, 0, null, purchaseOrderNumber, BigDecimal.ZERO, BigDecimal.ZERO,
			BigDecimal.ZERO, CommerceOrderConstants.PAYMENT_STATUS_PENDING,
			CommerceOrderConstants.ORDER_STATUS_OPEN, serviceContext);
	}

	@Override
	public CommerceOrder addUserCommerceOrder(long groupId, long userId)
		throws PortalException {

		return commerceOrderLocalService.addUserCommerceOrder(
			groupId, userId, userId);
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceOrder addUserCommerceOrder(
			long groupId, long userId, long orderUserId)
		throws PortalException {

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(groupId);
		serviceContext.setUserId(userId);

		if (hasWorkflowDefinition(
				groupId, CommerceOrderConstants.TYPE_PK_APPROVAL)) {

			serviceContext.setWorkflowAction(
				WorkflowConstants.ACTION_SAVE_DRAFT);
		}

		return addCommerceOrder(
			groupId, 0, orderUserId, 0, 0, 0, 0, 0, null, null, BigDecimal.ZERO,
			BigDecimal.ZERO, BigDecimal.ZERO,
			CommerceOrderConstants.PAYMENT_STATUS_PENDING,
			CommerceOrderConstants.ORDER_STATUS_OPEN, serviceContext);
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceOrder approveCommerceOrder(long userId, long commerceOrderId)
		throws PortalException {

		// Commerce order

		CommerceOrder commerceOrder = commerceOrderLocalService.updateStatus(
			userId, commerceOrderId, WorkflowConstants.STATUS_APPROVED,
			new ServiceContext(), null);

		// Workflow

		workflowInstanceLinkLocalService.deleteWorkflowInstanceLinks(
			commerceOrder.getCompanyId(), commerceOrder.getGroupId(),
			CommerceOrder.class.getName(), commerceOrder.getCommerceOrderId());

		return commerceOrder;
	}

	@Override
	public CommerceOrder cancelCommerceOrderPayment(
			long commerceOrderId, ServiceContext serviceContext)
		throws PortalException {

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		CommercePaymentEngine commercePaymentEngine = getCommercePaymentEngine(
			commerceOrder);

		if (commercePaymentEngine == null) {
			return commerceOrder;
		}

		String content = null;
		int status = CommerceOrderPaymentConstants.STATUS_CANCELLED;

		try {
			CommercePaymentEngineResult commercePaymentEngineResult =
				commercePaymentEngine.cancelPayment(
					commerceOrder, serviceContext);

			content = commercePaymentEngineResult.getContent();
		}
		catch (CommercePaymentEngineException cpee) {
			_log.error(
				"Unable to cancel payment of order " +
					commerceOrder.getCommerceOrderId(),
				cpee);

			content = getCommerceOrderPaymentContent(cpee);
		}

		commerceOrderPaymentLocalService.addCommerceOrderPayment(
			commerceOrder.getCommerceOrderId(), status, content,
			serviceContext);

		return commerceOrder;
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceOrder checkoutCommerceOrder(
			long commerceOrderId, CommerceContext commerceContext,
			ServiceContext serviceContext)
		throws PortalException {

		// Commerce order

		CommerceOrder commerceOrder =
			commerceOrderLocalService.recalculatePrice(
				commerceOrderId, commerceContext);

		commerceOrder = commerceOrderLocalService.approveCommerceOrder(
			serviceContext.getUserId(), commerceOrderId);

		validateCheckout(commerceOrder);

		serviceContext.setScopeGroupId(commerceOrder.getGroupId());

		CommerceOrderPrice commerceOrderPrice =
			_commerceOrderPriceCalculation.getCommerceOrderPrice(
				commerceOrder, commerceContext);

		CommerceMoney subtotal = commerceOrderPrice.getSubtotal();
		CommerceDiscountValue subtotalDiscountValue =
			commerceOrderPrice.getSubtotalDiscountValue();

		BigDecimal subtotalPrice = subtotal.getPrice();

		if (subtotalDiscountValue != null) {
			CommerceMoney subtotalDiscountAmount =
				subtotalDiscountValue.getDiscountAmount();

			subtotalPrice = subtotalPrice.subtract(
				subtotalDiscountAmount.getPrice());
		}

		CommerceMoney total = commerceOrderPrice.getTotal();
		CommerceDiscountValue totalDiscountValue =
			commerceOrderPrice.getTotalDiscountValue();

		BigDecimal totalPrice = total.getPrice();

		if (totalDiscountValue != null) {
			CommerceMoney totalDiscountAmount =
				totalDiscountValue.getDiscountAmount();

			totalPrice = totalPrice.subtract(totalDiscountAmount.getPrice());
		}

		commerceOrder.setSubtotal(subtotalPrice);
		commerceOrder.setTotal(totalPrice);
		commerceOrder.setOrderStatus(
			CommerceOrderConstants.ORDER_STATUS_IN_PROGRESS);

		// Commerce addresses

		long billingAddressId = commerceOrder.getBillingAddressId();

		if (billingAddressId > 0) {
			CommerceAddress commerceAddress =
				commerceAddressLocalService.copyCommerceAddress(
					billingAddressId, commerceOrder.getModelClassName(),
					commerceOrder.getCommerceOrderId(), serviceContext);

			billingAddressId = commerceAddress.getCommerceAddressId();
		}

		long shippingAddressId = commerceOrder.getShippingAddressId();

		if (shippingAddressId > 0) {
			CommerceAddress commerceAddress =
				commerceAddressLocalService.copyCommerceAddress(
					shippingAddressId, commerceOrder.getModelClassName(),
					commerceOrder.getCommerceOrderId(), serviceContext);

			shippingAddressId = commerceAddress.getCommerceAddressId();
		}

		if ((billingAddressId > 0) || (shippingAddressId > 0)) {
			commerceOrder.setBillingAddressId(billingAddressId);
			commerceOrder.setShippingAddressId(shippingAddressId);
		}

		return commerceOrderPersistence.update(commerceOrder);
	}

	@Override
	public CommerceOrder completeCommerceOrderPayment(
			long commerceOrderId, ServiceContext serviceContext)
		throws PortalException {

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		CommercePaymentEngine commercePaymentEngine = getCommercePaymentEngine(
			commerceOrder);

		if (commercePaymentEngine == null) {
			return commerceOrder;
		}

		String content = null;
		int status = CommerceOrderPaymentConstants.STATUS_COMPLETED;

		try {
			CommercePaymentEngineResult commercePaymentEngineResult =
				commercePaymentEngine.completePayment(
					commerceOrder, serviceContext);

			content = commercePaymentEngineResult.getContent();

			commerceOrder = updatePaymentStatus(
				commerceOrder.getCommerceOrderId(),
				CommerceOrderConstants.PAYMENT_STATUS_PAID, serviceContext);
		}
		catch (CommercePaymentEngineException cpee) {
			_log.error(
				"Unable to complete payment of order " +
					commerceOrder.getCommerceOrderId(),
				cpee);

			content = getCommerceOrderPaymentContent(cpee);
			status = CommerceOrderPaymentConstants.STATUS_FAILED;
		}

		commerceOrderPaymentLocalService.addCommerceOrderPayment(
			commerceOrder.getCommerceOrderId(), status, content,
			serviceContext);

		return commerceOrder;
	}

	@Indexable(type = IndexableType.DELETE)
	@Override
	public CommerceOrder deleteCommerceOrder(CommerceOrder commerceOrder)
		throws PortalException {

		// Commerce order items

		commerceOrderItemLocalService.deleteCommerceOrderItems(
			commerceOrder.getCommerceOrderId());

		// Commerce order notes

		commerceOrderNoteLocalService.deleteCommerceOrderNotes(
			commerceOrder.getCommerceOrderId());

		// Commerce order payments

		commerceOrderPaymentLocalService.deleteCommerceOrderPayments(
			commerceOrder.getCommerceOrderId());

		// Commerce addresses

		commerceAddressLocalService.deleteCommerceAddresses(
			commerceOrder.getModelClassName(),
			commerceOrder.getCommerceOrderId());

		// Commerce order

		commerceOrderPersistence.remove(commerceOrder);

		// Expando

		expandoRowLocalService.deleteRows(commerceOrder.getCommerceOrderId());

		// Workflow

		workflowInstanceLinkLocalService.deleteWorkflowInstanceLinks(
			commerceOrder.getCompanyId(), commerceOrder.getGroupId(),
			CommerceOrder.class.getName(), commerceOrder.getCommerceOrderId());

		return commerceOrder;
	}

	@Override
	public CommerceOrder deleteCommerceOrder(long commerceOrderId)
		throws PortalException {

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		return commerceOrderLocalService.deleteCommerceOrder(commerceOrder);
	}

	@Override
	public void deleteCommerceOrders(long groupId) throws PortalException {
		List<CommerceOrder> commerceOrders =
			commerceOrderPersistence.findByGroupId(groupId);

		for (CommerceOrder commerceOrder : commerceOrders) {
			commerceOrderLocalService.deleteCommerceOrder(commerceOrder);
		}
	}

	@Override
	public void deleteCommerceOrders(long userId, Date date, int status) {
		commerceOrderPersistence.removeByU_LtC_O(userId, date, status);
	}

	@Override
	public CommerceOrder executeWorkflowTransition(
			long userId, long commerceOrderId, long workflowTaskId,
			String transitionName, String comment)
		throws PortalException {

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		long companyId = commerceOrder.getCompanyId();

		WorkflowTask workflowTask = _workflowTaskManager.getWorkflowTask(
			companyId, workflowTaskId);

		if (!workflowTask.isAssignedToSingleUser()) {
			workflowTask = _workflowTaskManager.assignWorkflowTaskToUser(
				companyId, userId, workflowTask.getWorkflowTaskId(), userId,
				comment, null, null);
		}

		_workflowTaskManager.completeWorkflowTask(
			companyId, userId, workflowTask.getWorkflowTaskId(), transitionName,
			comment, null);

		return commerceOrder;
	}

	@Override
	public CommerceOrder fetchCommerceOrder(
		long groupId, long userId, int orderStatus) {

		return commerceOrderPersistence.fetchByG_U_O_First(
			groupId, userId, orderStatus, null);
	}

	@Override
	public int[] getAvailableOrderStatuses(long commerceOrderId)
		throws PortalException {

		if (commerceOrderId <= 0) {
			return AVAILABLE_ORDER_STATUSES;
		}

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		if (!commerceOrder.isPending() &&
			ArrayUtil.contains(
				AVAILABLE_ORDER_STATUSES, commerceOrder.getOrderStatus())) {

			return AVAILABLE_ORDER_STATUSES;
		}

		return new int[] {commerceOrder.getOrderStatus()};
	}

	@Override
	public List<CommerceOrder> getCommerceOrders(
		long groupId, int start, int end,
		OrderByComparator<CommerceOrder> orderByComparator) {

		return commerceOrderPersistence.findByGroupId(
			groupId, start, end, orderByComparator);
	}

	@Override
	public List<CommerceOrder> getCommerceOrders(
		long siteGroupId, int[] orderStatuses) {

		return commerceOrderFinder.findByS_O(siteGroupId, orderStatuses);
	}

	@Override
	public List<CommerceOrder> getCommerceOrders(
		long groupId, long orderUserId, int start, int end,
		OrderByComparator<CommerceOrder> orderByComparator) {

		return commerceOrderPersistence.findByG_O(
			groupId, orderUserId, start, end, orderByComparator);
	}

	@Override
	public List<CommerceOrder> getCommerceOrdersByBillingAddress(
		long billingAddressId) {

		return commerceOrderPersistence.findByBillingAddressId(
			billingAddressId);
	}

	@Override
	public List<CommerceOrder> getCommerceOrdersByShippingAddress(
		long shippingAddressId) {

		return commerceOrderPersistence.findByShippingAddressId(
			shippingAddressId);
	}

	@Override
	public int getCommerceOrdersCount(long groupId) {
		return commerceOrderPersistence.countByGroupId(groupId);
	}

	@Override
	public int getCommerceOrdersCount(long groupId, long orderUserId) {
		return commerceOrderPersistence.countByG_O(groupId, orderUserId);
	}

	@Override
	public void mergeGuestCommerceOrder(
			long guestCommerceOrderId, long userCommerceOrderId,
			CommerceContext commerceContext, ServiceContext serviceContext)
		throws PortalException {

		List<CommerceOrderItem> guestCommerceOrderItems =
			commerceOrderItemPersistence.findByCommerceOrderId(
				guestCommerceOrderId);

		for (CommerceOrderItem guestCommerceOrderItem :
				guestCommerceOrderItems) {

			List<CommerceOrderItem> userCommerceOrderItems =
				commerceOrderItemPersistence.findByC_I(
					userCommerceOrderId,
					guestCommerceOrderItem.getCPInstanceId());

			if (!userCommerceOrderItems.isEmpty()) {
				boolean found = false;

				for (CommerceOrderItem userCommerceOrderItem :
						userCommerceOrderItems) {

					if (_ddmFormValuesHelper.equals(
							guestCommerceOrderItem.getJson(),
							userCommerceOrderItem.getJson())) {

						found = true;

						break;
					}
				}

				if (found) {
					break;
				}
			}

			commerceOrderItemLocalService.addCommerceOrderItem(
				userCommerceOrderId, guestCommerceOrderItem.getCPInstanceId(),
				guestCommerceOrderItem.getQuantity(),
				guestCommerceOrderItem.getShippedQuantity(),
				guestCommerceOrderItem.getJson(), commerceContext,
				serviceContext);
		}

		commerceOrderLocalService.deleteCommerceOrder(guestCommerceOrderId);
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceOrder recalculatePrice(
			long commerceOrderId, CommerceContext commerceContext)
		throws PortalException {

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		for (CommerceOrderItem commerceOrderItem :
				commerceOrder.getCommerceOrderItems()) {

			commerceOrderItemLocalService.updateCommerceOrderItemPrice(
				commerceOrderItem.getCommerceOrderItemId(), commerceContext);
		}

		CommerceOrderPrice commerceOrderPrice =
			_commerceOrderPriceCalculation.getCommerceOrderPrice(
				commerceOrder, commerceContext);

		CommerceMoney shippingValue = commerceOrderPrice.getShippingValue();
		CommerceMoney subtotal = commerceOrderPrice.getSubtotal();
		CommerceMoney taxValue = commerceOrderPrice.getTaxValue();
		CommerceMoney total = commerceOrderPrice.getTotal();

		commerceOrder.setShippingAmount(shippingValue.getPrice());
		commerceOrder.setSubtotal(subtotal.getPrice());
		commerceOrder.setTaxAmount(taxValue.getPrice());
		commerceOrder.setTotal(total.getPrice());

		CommerceDiscountValue shippingDiscountValue =
			commerceOrderPrice.getShippingDiscountValue();
		CommerceDiscountValue subtotalDiscountValue =
			commerceOrderPrice.getSubtotalDiscountValue();
		CommerceDiscountValue totalDiscountValue =
			commerceOrderPrice.getTotalDiscountValue();

		_setCommerceOrderShippingDiscountValue(
			commerceOrder, shippingDiscountValue);
		_setCommerceOrderSubtotalDiscountValue(
			commerceOrder, subtotalDiscountValue);
		_setCommerceOrderTotalDiscountValue(commerceOrder, totalDiscountValue);

		return commerceOrder;
	}

	@Override
	public CommerceOrder reorderCommerceOrder(
			long userId, long commerceOrderId, CommerceContext commerceContext)
		throws PortalException {

		// Commerce order

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(commerceOrder.getGroupId());
		serviceContext.setUserId(userId);

		long billingAddressId = 0;
		long shippingAddressId = 0;

		CommerceAddress billingAddress = getNewCommerceAddress(
			commerceOrder, commerceOrder.getBillingAddress(), serviceContext);
		CommerceAddress shippingAddress = getNewCommerceAddress(
			commerceOrder, commerceOrder.getShippingAddress(), serviceContext);

		if (billingAddress != null) {
			billingAddressId = billingAddress.getCommerceAddressId();
		}

		if (shippingAddress != null) {
			shippingAddressId = shippingAddress.getCommerceAddressId();
		}

		CommerceOrder newCommerceOrder = addCommerceOrder(
			commerceOrder.getSiteGroupId(),
			commerceOrder.getOrderOrganizationId(),
			commerceOrder.getOrderUserId(),
			commerceOrder.getCommerceCurrencyId(), billingAddressId,
			shippingAddressId, commerceOrder.getCommercePaymentMethodId(),
			commerceOrder.getCommerceShippingMethodId(),
			commerceOrder.getShippingOptionName(), StringPool.BLANK,
			commerceOrder.getSubtotal(), commerceOrder.getShippingAmount(),
			commerceOrder.getTotal(),
			CommerceOrderConstants.PAYMENT_STATUS_PENDING,
			CommerceOrderConstants.ORDER_STATUS_OPEN, serviceContext);

		// Commerce order items

		List<CommerceOrderItem> commerceOrderItems =
			commerceOrderItemLocalService.getCommerceOrderItems(
				commerceOrder.getCommerceOrderId(), QueryUtil.ALL_POS,
				QueryUtil.ALL_POS);

		for (CommerceOrderItem commerceOrderItem : commerceOrderItems) {
			commerceOrderItemLocalService.addCommerceOrderItem(
				newCommerceOrder.getCommerceOrderId(),
				commerceOrderItem.getCPInstanceId(),
				commerceOrderItem.getQuantity(), 0, commerceOrderItem.getJson(),
				commerceContext, serviceContext);
		}

		return newCommerceOrder;
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceOrder resetCommerceOrderShipping(long commerceOrderId)
		throws PortalException {

		CommerceOrder commerceOrder =
			commerceOrderLocalService.getCommerceOrder(commerceOrderId);

		commerceOrder.setCommerceShippingMethodId(0);
		commerceOrder.setShippingOptionName(null);
		commerceOrder.setShippingAmount(BigDecimal.ZERO);

		return commerceOrderPersistence.update(commerceOrder);
	}

	@Override
	public BaseModelSearchResult<CommerceOrder> searchCommerceOrders(
			SearchContext searchContext)
		throws PortalException {

		Indexer<CommerceOrder> indexer = IndexerRegistryUtil.nullSafeGetIndexer(
			CommerceOrder.class.getName());

		for (int i = 0; i < 10; i++) {
			Hits hits = indexer.search(searchContext);

			List<CommerceOrder> commerceOrders = getCommerceOrders(hits);

			if (commerceOrders != null) {
				return new BaseModelSearchResult<>(
					commerceOrders, hits.getLength());
			}
		}

		throw new SearchException(
			"Unable to fix the search index after 10 attempts");
	}

	@Override
	public long searchCommerceOrdersCount(SearchContext searchContext)
		throws PortalException {

		Indexer<CommerceOrder> indexer = IndexerRegistryUtil.nullSafeGetIndexer(
			CommerceOrder.class.getName());

		return indexer.searchCount(searchContext);
	}

	@Override
	public String startCommerceOrderPayment(
			long commerceOrderId, ServiceContext serviceContext)
		throws PortalException {

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		CommercePaymentEngine commercePaymentEngine = getCommercePaymentEngine(
			commerceOrder);

		if (commercePaymentEngine == null) {
			updatePaymentStatus(
				commerceOrder.getCommerceOrderId(),
				CommerceOrderConstants.PAYMENT_STATUS_PAID, serviceContext);

			return null;
		}

		StringBundler sb = new StringBundler(13);

		sb.append(serviceContext.getPortalURL());
		sb.append(_portal.getPathModule());
		sb.append(CharPool.SLASH);
		sb.append(CommerceConstants.PAYMENT_SERVLET_PATH);
		sb.append(CharPool.QUESTION);
		sb.append("groupId=");
		sb.append(commerceOrder.getGroupId());
		sb.append("&uuid=");
		sb.append(URLCodec.encodeURL(commerceOrder.getUuid()));

		String redirect = ParamUtil.getString(serviceContext, "redirect");

		if (Validator.isNotNull(redirect)) {
			sb.append("&redirect=");
			sb.append(URLCodec.encodeURL(redirect));
		}

		String returnURL = sb.toString();

		sb.append("&cancel=");
		sb.append(StringPool.TRUE);

		String cancelURL = sb.toString();

		String output = null;

		String content = null;
		int status = CommerceOrderPaymentConstants.STATUS_PENDING;

		try {
			CommercePaymentEngineResult.StartPayment startPayment =
				commercePaymentEngine.startPayment(
					commerceOrder, cancelURL, returnURL, serviceContext);

			content = startPayment.getContent();
			output = startPayment.getOutput();
		}
		catch (CommercePaymentEngineException cpee) {
			_log.error(
				"Unable to start payment of order " +
					commerceOrder.getCommerceOrderId(),
				cpee);

			content = getCommerceOrderPaymentContent(cpee);
			status = CommerceOrderPaymentConstants.STATUS_FAILED;
		}

		if ((status == CommerceOrderPaymentConstants.STATUS_FAILED) ||
			Validator.isNotNull(content)) {

			commerceOrderPaymentLocalService.addCommerceOrderPayment(
				commerceOrder.getCommerceOrderId(), status, content,
				serviceContext);
		}

		if ((status == CommerceOrderPaymentConstants.STATUS_PENDING) &&
			Validator.isNull(output)) {

			setCommerceOrderToTransmit(commerceOrder, serviceContext);
		}

		return output;
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceOrder submitCommerceOrder(long userId, long commerceOrderId)
		throws PortalException {

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		if (!commerceOrder.isDraft() || commerceOrder.isEmpty()) {
			return commerceOrder;
		}

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(commerceOrder.getGroupId());
		serviceContext.setUserId(userId);
		serviceContext.setWorkflowAction(WorkflowConstants.ACTION_PUBLISH);

		return startWorkflowInstance(
			serviceContext.getUserId(), commerceOrder, serviceContext);
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceOrder updateBillingAddress(
			long commerceOrderId, String name, String description,
			String street1, String street2, String street3, String city,
			String zip, long commerceRegionId, long commerceCountryId,
			String phoneNumber, ServiceContext serviceContext)
		throws PortalException {

		return updateAddress(
			commerceOrderId, name, description, street1, street2, street3, city,
			zip, commerceRegionId, commerceCountryId, phoneNumber,
			CommerceOrder::getBillingAddressId,
			CommerceOrder::setBillingAddressId, serviceContext);
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceOrder updateCommerceOrder(
			long commerceOrderId, long billingAddressId, long shippingAddressId,
			long commercePaymentMethodId, long commerceShippingMethodId,
			String shippingOptionName, String purchaseOrderNumber,
			BigDecimal subtotal, BigDecimal shippingAmount, BigDecimal total,
			String advanceStatus, CommerceContext commerceContext)
		throws PortalException {

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		commerceOrder.setBillingAddressId(billingAddressId);
		commerceOrder.setShippingAddressId(shippingAddressId);
		commerceOrder.setCommercePaymentMethodId(commercePaymentMethodId);
		commerceOrder.setCommerceShippingMethodId(commerceShippingMethodId);
		commerceOrder.setShippingOptionName(shippingOptionName);
		commerceOrder.setPurchaseOrderNumber(purchaseOrderNumber);
		commerceOrder.setSubtotal(subtotal);

		if (commerceContext != null) {
			CommerceOrderPrice commerceOrderPrice =
				_commerceOrderPriceCalculation.getCommerceOrderPrice(
					commerceOrder, commerceContext);

			CommerceDiscountValue shippingDiscountValue =
				commerceOrderPrice.getShippingDiscountValue();

			if (shippingDiscountValue != null) {
				CommerceMoney shippingDiscountAmount =
					shippingDiscountValue.getDiscountAmount();

				shippingAmount = shippingAmount.subtract(
					shippingDiscountAmount.getPrice());
			}
		}

		commerceOrder.setShippingAmount(shippingAmount);
		commerceOrder.setTotal(total);
		commerceOrder.setAdvanceStatus(advanceStatus);

		commerceOrderPersistence.update(commerceOrder);

		return commerceOrder;
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceOrder updateOrderStatus(
			long commerceOrderId, int orderStatus)
		throws PortalException {

		// Commerce order

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		final int previousOrderStatus = commerceOrder.getOrderStatus();

		validateOrderStatus(commerceOrder.getCommerceOrderId(), orderStatus);

		commerceOrder.setOrderStatus(orderStatus);

		commerceOrderPersistence.update(commerceOrder);

		// Messaging

		sendMessage(
			commerceOrder.getCommerceOrderId(), commerceOrder.getOrderStatus(),
			previousOrderStatus);

		return commerceOrder;
	}

	@Override
	public CommerceOrder updatePaymentStatus(
			long commerceOrderId, int paymentStatus,
			ServiceContext serviceContext)
		throws PortalException {

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		commerceOrder.setPaymentStatus(paymentStatus);

		commerceOrderPersistence.update(commerceOrder);

		if ((commerceOrder.getOrderStatus() ==
				CommerceOrderConstants.ORDER_STATUS_IN_PROGRESS) &&
			(commerceOrder.getPaymentStatus() ==
				CommerceOrderConstants.PAYMENT_STATUS_PAID)) {

			commerceOrder = setCommerceOrderToTransmit(
				commerceOrder, serviceContext);
		}

		return commerceOrder;
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceOrder updatePurchaseOrderNumber(
			long commerceOrderId, String purchaseOrderNumber)
		throws PortalException {

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		validatePurchaseOrderNumber(purchaseOrderNumber);

		commerceOrder.setPurchaseOrderNumber(purchaseOrderNumber);

		commerceOrderPersistence.update(commerceOrder);

		return commerceOrder;
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceOrder updateShippingAddress(
			long commerceOrderId, String name, String description,
			String street1, String street2, String street3, String city,
			String zip, long commerceRegionId, long commerceCountryId,
			String phoneNumber, ServiceContext serviceContext)
		throws PortalException {

		return updateAddress(
			commerceOrderId, name, description, street1, street2, street3, city,
			zip, commerceRegionId, commerceCountryId, phoneNumber,
			CommerceOrder::getShippingAddressId,
			CommerceOrder::setShippingAddressId, serviceContext);
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceOrder updateShippingMethod(
			long commerceOrderId, long commerceShippingMethodId,
			String shippingOptionName, BigDecimal shippingAmount,
			CommerceContext commerceContext)
		throws PortalException {

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		commerceOrder.setCommerceShippingMethodId(commerceShippingMethodId);
		commerceOrder.setShippingOptionName(shippingOptionName);
		commerceOrder.setShippingAmount(shippingAmount);

		commerceOrderPersistence.update(commerceOrder);

		commerceOrder = commerceOrderLocalService.recalculatePrice(
			commerceOrder.getCommerceOrderId(), commerceContext);

		return commerceOrder;
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public CommerceOrder updateStatus(
			long userId, long commerceOrderId, int status,
			ServiceContext serviceContext,
			Map<String, Serializable> workflowContext)
		throws PortalException {

		User user = userLocalService.getUser(userId);
		Date now = new Date();

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		commerceOrder.setStatus(status);
		commerceOrder.setStatusByUserId(user.getUserId());
		commerceOrder.setStatusByUserName(user.getFullName());
		commerceOrder.setStatusDate(serviceContext.getModifiedDate(now));

		commerceOrderPersistence.update(commerceOrder);

		return commerceOrder;
	}

	@Override
	public CommerceOrder updateUser(long commerceOrderId, long userId)
		throws PortalException {

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		User user = userLocalService.getUser(userId);

		commerceOrder.setUserId(user.getUserId());
		commerceOrder.setUserName(user.getFullName());

		return commerceOrderPersistence.update(commerceOrder);
	}

	protected String getCommerceOrderPaymentContent(
		CommercePaymentEngineException cpee) {

		return StackTraceUtil.getStackTrace(cpee);
	}

	protected List<CommerceOrder> getCommerceOrders(Hits hits)
		throws PortalException {

		List<Document> documents = hits.toList();

		List<CommerceOrder> commerceOrders = new ArrayList<>(documents.size());

		for (Document document : documents) {
			long commerceOrderId = GetterUtil.getLong(
				document.get(Field.ENTRY_CLASS_PK));

			CommerceOrder commerceOrder = fetchCommerceOrder(commerceOrderId);

			if (commerceOrder == null) {
				commerceOrders = null;

				Indexer<CommerceOrder> indexer = IndexerRegistryUtil.getIndexer(
					CommerceOrder.class);

				long companyId = GetterUtil.getLong(
					document.get(Field.COMPANY_ID));

				indexer.delete(companyId, document.getUID());
			}
			else if (commerceOrders != null) {
				commerceOrders.add(commerceOrder);
			}
		}

		return commerceOrders;
	}

	protected CommercePaymentEngine getCommercePaymentEngine(
			CommerceOrder commerceOrder)
		throws PortalException {

		CommercePaymentMethod commercePaymentMethod =
			commerceOrder.getCommercePaymentMethod();

		if (commercePaymentMethod == null) {
			return null;
		}

		if (!commercePaymentMethod.isActive()) {
			throw new NoSuchPaymentMethodException(
				"Payment method " + commercePaymentMethod + " is not active");
		}

		return _commercePaymentEngineRegistry.getCommercePaymentEngine(
			commercePaymentMethod.getEngineKey());
	}

	protected CommerceAddress getNewCommerceAddress(
			CommerceOrder commerceOrder, CommerceAddress commerceAddress,
			ServiceContext serviceContext)
		throws PortalException {

		if (commerceAddress == null) {
			return commerceAddress;
		}

		long groupId = serviceContext.getScopeGroupId();
		String className = commerceOrder.getClassName();
		long classPK = commerceOrder.getClassPK();

		List<CommerceAddress> commerceAddresses =
			commerceAddressLocalService.getCommerceAddresses(
				groupId, className, classPK);

		for (CommerceAddress newCommerceAddress : commerceAddresses) {
			if (commerceAddress.isSameAddress(newCommerceAddress)) {
				return newCommerceAddress;
			}
		}

		return commerceAddressLocalService.copyCommerceAddress(
			commerceAddress.getCommerceAddressId(), className, classPK,
			serviceContext);
	}

	protected boolean hasWorkflowDefinition(long siteGroupId, long typePK)
		throws PortalException {

		Group group = groupLocalService.getGroup(siteGroupId);

		return workflowDefinitionLinkLocalService.hasWorkflowDefinitionLink(
			group.getCompanyId(), group.getGroupId(),
			CommerceOrder.class.getName(), 0, typePK);
	}

	protected void sendMessage(
		long commerceOrderId, int orderStatus, int previousOrderStatus) {

		TransactionCommitCallbackUtil.registerCallback(
			new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					Message message = new Message();

					message.put("commerceOrderId", commerceOrderId);
					message.put("orderStatus", orderStatus);
					message.put("previousOrderStatus", previousOrderStatus);

					MessageBusUtil.sendMessage(
						CommerceDestinationNames.ORDER_STATUS, message);

					return null;
				}

			});
	}

	protected CommerceOrder setCommerceOrderToTransmit(
			CommerceOrder commerceOrder, ServiceContext serviceContext)
		throws PortalException {

		// Commerce order

		int previousOrderStatus = commerceOrder.getOrderStatus();

		commerceOrder.setOrderStatus(
			CommerceOrderConstants.ORDER_STATUS_TO_TRANSMIT);
		commerceOrder.setStatus(WorkflowConstants.STATUS_PENDING);

		commerceOrderPersistence.update(commerceOrder);

		// Messaging

		sendMessage(
			commerceOrder.getCommerceOrderId(), commerceOrder.getOrderStatus(),
			previousOrderStatus);

		// Workflow

		serviceContext.setScopeGroupId(commerceOrder.getGroupId());

		return startWorkflowInstance(
			serviceContext.getUserId(), commerceOrder, serviceContext);
	}

	protected CommerceOrder startWorkflowInstance(
			long userId, CommerceOrder commerceOrder,
			ServiceContext serviceContext)
		throws PortalException {

		Map<String, Serializable> workflowContext = new HashMap<>();

		return WorkflowHandlerRegistryUtil.startWorkflowInstance(
			commerceOrder.getCompanyId(), commerceOrder.getGroupId(), userId,
			CommerceOrder.class.getName(), commerceOrder.getCommerceOrderId(),
			commerceOrder, serviceContext, workflowContext);
	}

	protected CommerceOrder updateAddress(
			long commerceOrderId, String name, String description,
			String street1, String street2, String street3, String city,
			String zip, long commerceRegionId, long commerceCountryId,
			String phoneNumber,
			Function<CommerceOrder, Long> commerceAddressIdGetter,
			BiConsumer<CommerceOrder, Long> commerceAddressIdSetter,
			ServiceContext serviceContext)
		throws PortalException {

		CommerceOrder commerceOrder = commerceOrderPersistence.findByPrimaryKey(
			commerceOrderId);

		CommerceAddress commerceAddress = null;

		long commerceAddressId = commerceAddressIdGetter.apply(commerceOrder);

		if (commerceAddressId > 0) {
			commerceAddress = commerceAddressLocalService.updateCommerceAddress(
				commerceAddressId, name, description, street1, street2, street3,
				city, zip, commerceRegionId, commerceCountryId, phoneNumber,
				false, false, serviceContext);
		}
		else {
			commerceAddress = commerceAddressLocalService.addCommerceAddress(
				commerceOrder.getModelClassName(),
				commerceOrder.getCommerceOrderId(), name, description, street1,
				street2, street3, city, zip, commerceRegionId,
				commerceCountryId, phoneNumber, false, false, serviceContext);
		}

		commerceAddressIdSetter.accept(
			commerceOrder, commerceAddress.getCommerceAddressId());

		return commerceOrderPersistence.update(commerceOrder);
	}

	protected void validateCheckout(CommerceOrder commerceOrder)
		throws PortalException {

		if (commerceOrder.isDraft() || !commerceOrder.isOpen()) {
			throw new CommerceOrderStatusException();
		}

		CommercePaymentMethod commercePaymentMethod = null;

		long commercePaymentMethodId =
			commerceOrder.getCommercePaymentMethodId();

		if (commercePaymentMethodId > 0) {
			commercePaymentMethod =
				commercePaymentMethodLocalService.getCommercePaymentMethod(
					commercePaymentMethodId);

			if (!commercePaymentMethod.isActive()) {
				commercePaymentMethod = null;
			}
			else if (!commerceOrder.isB2B() &&
					 (commerceOrder.getBillingAddressId() <= 0)) {

				throw new CommerceOrderBillingAddressException();
			}
		}

		if ((commercePaymentMethod == null) &&
			(commercePaymentMethodLocalService.getCommercePaymentMethodsCount(
				commerceOrder.getGroupId(), true) > 0)) {

			throw new CommerceOrderPaymentMethodException();
		}

		CommerceShippingMethod commerceShippingMethod = null;

		long commerceShippingMethodId =
			commerceOrder.getCommerceShippingMethodId();

		if (commerceShippingMethodId > 0) {
			commerceShippingMethod =
				commerceShippingMethodLocalService.getCommerceShippingMethod(
					commerceShippingMethodId);

			if (!commerceShippingMethod.isActive()) {
				commerceShippingMethod = null;
			}
			else if (commerceOrder.getShippingAddressId() <= 0) {
				throw new CommerceOrderShippingAddressException();
			}
		}

		if ((commerceShippingMethod == null) &&
			(commerceShippingMethodLocalService.getCommerceShippingMethodsCount(
				commerceOrder.getGroupId(), true) > 0) &&
			_commerceShippingHelper.isShippable(commerceOrder)) {

			throw new CommerceOrderShippingMethodException();
		}
	}

	protected void validateGuestOrders() throws PortalException {
		int count = commerceOrderPersistence.countByUserId(
			UserConstants.USER_ID_DEFAULT);

		if (count >= _commerceOrderConfiguration.guestCartMaxAllowed()) {
			throw new GuestCartMaxAllowedException();
		}
	}

	protected void validateOrderStatus(long commerceOrderId, int orderStatus)
		throws PortalException {

		int[] availableOrderStatuses = getAvailableOrderStatuses(
			commerceOrderId);

		if (!ArrayUtil.contains(availableOrderStatuses, orderStatus)) {
			throw new CommerceOrderStatusException();
		}
	}

	protected void validatePurchaseOrderNumber(String purchaseOrderNumber)
		throws PortalException {

		if (Validator.isNull(purchaseOrderNumber)) {
			throw new CommerceOrderPurchaseOrderNumberException();
		}
	}

	protected static final int[] AVAILABLE_ORDER_STATUSES = {
		CommerceOrderConstants.ORDER_STATUS_TO_TRANSMIT,
		CommerceOrderConstants.ORDER_STATUS_TRANSMITTED,
		CommerceOrderConstants.ORDER_STATUS_AWAITING_FULFILLMENT,
		CommerceOrderConstants.ORDER_STATUS_AWAITING_PICKUP,
		CommerceOrderConstants.ORDER_STATUS_AWAITING_SHIPMENT,
		CommerceOrderConstants.ORDER_STATUS_PARTIALLY_REFUNDED,
		CommerceOrderConstants.ORDER_STATUS_PARTIALLY_SHIPPED,
		CommerceOrderConstants.ORDER_STATUS_REFUNDED,
		CommerceOrderConstants.ORDER_STATUS_SHIPPED,
		CommerceOrderConstants.ORDER_STATUS_COMPLETED,
		CommerceOrderConstants.ORDER_STATUS_CANCELLED,
		CommerceOrderConstants.ORDER_STATUS_DECLINED,
		CommerceOrderConstants.ORDER_STATUS_DISPUTED
	};

	private void _setCommerceOrderShippingDiscountValue(
		CommerceOrder commerceOrder,
		CommerceDiscountValue commerceDiscountValue) {

		BigDecimal discountAmount = BigDecimal.ZERO;
		BigDecimal discountPercentageLevel1 = BigDecimal.ZERO;
		BigDecimal discountPercentageLevel2 = BigDecimal.ZERO;
		BigDecimal discountPercentageLevel3 = BigDecimal.ZERO;
		BigDecimal discountPercentageLevel4 = BigDecimal.ZERO;

		if (commerceDiscountValue != null) {
			CommerceMoney discountAmountCommerceMoney =
				commerceDiscountValue.getDiscountAmount();

			discountAmount = discountAmountCommerceMoney.getPrice();

			BigDecimal[] percentages = commerceDiscountValue.getPercentages();

			if (percentages.length >= 1) {
				discountPercentageLevel1 = percentages[0];
			}

			if (percentages.length >= 2) {
				discountPercentageLevel1 = percentages[1];
			}

			if (percentages.length >= 3) {
				discountPercentageLevel1 = percentages[2];
			}

			if (percentages.length >= 4) {
				discountPercentageLevel1 = percentages[3];
			}
		}

		commerceOrder.setShippingDiscountAmount(discountAmount);
		commerceOrder.setShippingDiscountPercentageLevel1(
			discountPercentageLevel1);
		commerceOrder.setShippingDiscountPercentageLevel2(
			discountPercentageLevel2);
		commerceOrder.setShippingDiscountPercentageLevel3(
			discountPercentageLevel3);
		commerceOrder.setShippingDiscountPercentageLevel4(
			discountPercentageLevel4);
	}

	private void _setCommerceOrderSubtotalDiscountValue(
		CommerceOrder commerceOrder,
		CommerceDiscountValue commerceDiscountValue) {

		BigDecimal discountAmount = BigDecimal.ZERO;
		BigDecimal discountPercentageLevel1 = BigDecimal.ZERO;
		BigDecimal discountPercentageLevel2 = BigDecimal.ZERO;
		BigDecimal discountPercentageLevel3 = BigDecimal.ZERO;
		BigDecimal discountPercentageLevel4 = BigDecimal.ZERO;

		if (commerceDiscountValue != null) {
			CommerceMoney discountAmountCommerceMoney =
				commerceDiscountValue.getDiscountAmount();

			discountAmount = discountAmountCommerceMoney.getPrice();

			BigDecimal[] percentages = commerceDiscountValue.getPercentages();

			if (percentages.length >= 1) {
				discountPercentageLevel1 = percentages[0];
			}

			if (percentages.length >= 2) {
				discountPercentageLevel1 = percentages[1];
			}

			if (percentages.length >= 3) {
				discountPercentageLevel1 = percentages[2];
			}

			if (percentages.length >= 4) {
				discountPercentageLevel1 = percentages[3];
			}
		}

		commerceOrder.setSubtotalDiscountAmount(discountAmount);
		commerceOrder.setSubtotalDiscountPercentageLevel1(
			discountPercentageLevel1);
		commerceOrder.setSubtotalDiscountPercentageLevel2(
			discountPercentageLevel2);
		commerceOrder.setSubtotalDiscountPercentageLevel3(
			discountPercentageLevel3);
		commerceOrder.setSubtotalDiscountPercentageLevel4(
			discountPercentageLevel4);
	}

	private void _setCommerceOrderTotalDiscountValue(
		CommerceOrder commerceOrder,
		CommerceDiscountValue commerceDiscountValue) {

		BigDecimal discountAmount = BigDecimal.ZERO;
		BigDecimal discountPercentageLevel1 = BigDecimal.ZERO;
		BigDecimal discountPercentageLevel2 = BigDecimal.ZERO;
		BigDecimal discountPercentageLevel3 = BigDecimal.ZERO;
		BigDecimal discountPercentageLevel4 = BigDecimal.ZERO;

		if (commerceDiscountValue != null) {
			CommerceMoney discountAmountCommerceMoney =
				commerceDiscountValue.getDiscountAmount();

			discountAmount = discountAmountCommerceMoney.getPrice();

			BigDecimal[] percentages = commerceDiscountValue.getPercentages();

			if (percentages.length >= 1) {
				discountPercentageLevel1 = percentages[0];
			}

			if (percentages.length >= 2) {
				discountPercentageLevel1 = percentages[1];
			}

			if (percentages.length >= 3) {
				discountPercentageLevel1 = percentages[2];
			}

			if (percentages.length >= 4) {
				discountPercentageLevel1 = percentages[3];
			}
		}

		commerceOrder.setTotalDiscountAmount(discountAmount);
		commerceOrder.setTotalDiscountPercentageLevel1(
			discountPercentageLevel1);
		commerceOrder.setTotalDiscountPercentageLevel2(
			discountPercentageLevel2);
		commerceOrder.setTotalDiscountPercentageLevel3(
			discountPercentageLevel3);
		commerceOrder.setTotalDiscountPercentageLevel4(
			discountPercentageLevel4);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CommerceOrderLocalServiceImpl.class);

	@ServiceReference(type = CommerceCurrencyLocalService.class)
	private CommerceCurrencyLocalService _commerceCurrencyLocalService;

	@ServiceReference(type = CommerceOrderConfiguration.class)
	private CommerceOrderConfiguration _commerceOrderConfiguration;

	@ServiceReference(type = CommerceOrderPriceCalculation.class)
	private CommerceOrderPriceCalculation _commerceOrderPriceCalculation;

	@ServiceReference(type = CommerceOrganizationLocalService.class)
	private CommerceOrganizationLocalService _commerceOrganizationLocalService;

	@ServiceReference(type = CommercePaymentEngineRegistry.class)
	private CommercePaymentEngineRegistry _commercePaymentEngineRegistry;

	@ServiceReference(type = CommerceShippingHelper.class)
	private CommerceShippingHelper _commerceShippingHelper;

	@ServiceReference(type = DDMFormValuesHelper.class)
	private DDMFormValuesHelper _ddmFormValuesHelper;

	@ServiceReference(type = Portal.class)
	private Portal _portal;

	@ServiceReference(type = WorkflowTaskManager.class)
	private WorkflowTaskManager _workflowTaskManager;

}