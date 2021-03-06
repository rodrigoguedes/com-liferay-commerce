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

package com.liferay.commerce.discount;

import aQute.bnd.annotation.ProviderType;

import com.liferay.commerce.context.CommerceContext;
import com.liferay.portal.kernel.exception.PortalException;

import java.math.BigDecimal;

/**
 * @author Marco Leo
 */
@ProviderType
public interface CommerceDiscountCalculation {

	public CommerceDiscountValue getOrderShippingCommerceDiscountValue(
			BigDecimal orderShippingCost, CommerceContext commerceContext)
		throws PortalException;

	public CommerceDiscountValue getOrderSubtotalCommerceDiscountValue(
			BigDecimal orderSubtotal, CommerceContext commerceContext)
		throws PortalException;

	public CommerceDiscountValue getOrderTotalCommerceDiscountValue(
			BigDecimal orderTotal, CommerceContext commerceContext)
		throws PortalException;

	public CommerceDiscountValue getProductCommerceDiscountValue(
			long cpInstanceId, int quantity, BigDecimal productUnitPrice,
			CommerceContext commerceContext)
		throws PortalException;

}