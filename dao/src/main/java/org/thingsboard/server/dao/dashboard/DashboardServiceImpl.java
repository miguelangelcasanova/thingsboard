/**
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.dashboard;

import static org.thingsboard.server.dao.DaoUtil.convertDataList;
import static org.thingsboard.server.dao.DaoUtil.getData;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.BaseEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.*;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.service.Validator;

@Service
@Slf4j
public class DashboardServiceImpl extends BaseEntityService implements DashboardService {

    @Autowired
    private DashboardDao dashboardDao;

    @Autowired
    private DashboardInfoDao dashboardInfoDao;

    @Autowired
    private TenantDao tenantDao;
    
    @Autowired
    private CustomerDao customerDao;
    
    @Override
    public Dashboard findDashboardById(DashboardId dashboardId) {
        log.trace("Executing findDashboardById [{}]", dashboardId);
        Validator.validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
        DashboardEntity dashboardEntity = dashboardDao.findById(dashboardId.getId());
        return getData(dashboardEntity);
    }

    @Override
    public DashboardInfo findDashboardInfoById(DashboardId dashboardId) {
        log.trace("Executing findDashboardInfoById [{}]", dashboardId);
        Validator.validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
        DashboardInfoEntity dashboardInfoEntity = dashboardInfoDao.findById(dashboardId.getId());
        return getData(dashboardInfoEntity);
    }

    @Override
    public Dashboard saveDashboard(Dashboard dashboard) {
        log.trace("Executing saveDashboard [{}]", dashboard);
        dashboardValidator.validate(dashboard);
        DashboardEntity dashboardEntity = dashboardDao.save(dashboard);
        return getData(dashboardEntity);
    }
    
    @Override
    public Dashboard assignDashboardToCustomer(DashboardId dashboardId, CustomerId customerId) {
        Dashboard dashboard = findDashboardById(dashboardId);
        dashboard.setCustomerId(customerId);
        return saveDashboard(dashboard);
    }

    @Override
    public Dashboard unassignDashboardFromCustomer(DashboardId dashboardId) {
        Dashboard dashboard = findDashboardById(dashboardId);
        dashboard.setCustomerId(null);
        return saveDashboard(dashboard);
    }

    @Override
    public void deleteDashboard(DashboardId dashboardId) {
        log.trace("Executing deleteDashboard [{}]", dashboardId);
        Validator.validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
        deleteEntityRelations(dashboardId);
        dashboardDao.removeById(dashboardId.getId());
    }

    @Override
    public TextPageData<DashboardInfo> findDashboardsByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findDashboardsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<DashboardInfoEntity> dashboardEntities = dashboardInfoDao.findDashboardsByTenantId(tenantId.getId(), pageLink);
        List<DashboardInfo> dashboards = convertDataList(dashboardEntities);
        return new TextPageData<DashboardInfo>(dashboards, pageLink);
    }

    @Override
    public void deleteDashboardsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDashboardsByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        tenantDashboardsRemover.removeEntitites(tenantId);
    }

    @Override
    public TextPageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TextPageLink pageLink) {
        log.trace("Executing findDashboardsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<DashboardInfoEntity> dashboardEntities = dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
        List<DashboardInfo> dashboards = convertDataList(dashboardEntities);
        return new TextPageData<DashboardInfo>(dashboards, pageLink);
    }

    @Override
    public void unassignCustomerDashboards(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerDashboards, tenantId [{}], customerId [{}]", tenantId, customerId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        new CustomerDashboardsUnassigner(tenantId).removeEntitites(customerId);
    }
    
    private DataValidator<Dashboard> dashboardValidator =
            new DataValidator<Dashboard>() {
                @Override
                protected void validateDataImpl(Dashboard dashboard) {
                    if (StringUtils.isEmpty(dashboard.getTitle())) {
                        throw new DataValidationException("Dashboard title should be specified!");
                    }
                    if (dashboard.getTenantId() == null) {
                        throw new DataValidationException("Dashboard should be assigned to tenant!");
                    } else {
                        TenantEntity tenant = tenantDao.findById(dashboard.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Dashboard is referencing to non-existent tenant!");
                        }
                    }
                    if (dashboard.getCustomerId() == null) {
                        dashboard.setCustomerId(new CustomerId(ModelConstants.NULL_UUID));
                    } else if (!dashboard.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                        CustomerEntity customer = customerDao.findById(dashboard.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign dashboard to non-existent customer!");
                        }
                        if (!customer.getTenantId().equals(dashboard.getTenantId().getId())) {
                            throw new DataValidationException("Can't assign dashboard to customer from different tenant!");
                        }
                    }
                }
    };
    
    private PaginatedRemover<TenantId, DashboardInfoEntity> tenantDashboardsRemover =
            new PaginatedRemover<TenantId, DashboardInfoEntity>() {
        
        @Override
        protected List<DashboardInfoEntity> findEntities(TenantId id, TextPageLink pageLink) {
            return dashboardInfoDao.findDashboardsByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(DashboardInfoEntity entity) {
            deleteDashboard(new DashboardId(entity.getId()));
        }
    };
    
    class CustomerDashboardsUnassigner extends PaginatedRemover<CustomerId, DashboardInfoEntity> {
        
        private TenantId tenantId;
        
        CustomerDashboardsUnassigner(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        protected List<DashboardInfoEntity> findEntities(CustomerId id, TextPageLink pageLink) {
            return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(DashboardInfoEntity entity) {
            unassignDashboardFromCustomer(new DashboardId(entity.getId()));
        }
        
    }

}
