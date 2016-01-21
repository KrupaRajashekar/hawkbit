/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.tenancy;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.concurrent.Callable;

import org.eclipse.hawkbit.AbstractIntegrationTest;
import org.eclipse.hawkbit.WithSpringAuthorityRule;
import org.eclipse.hawkbit.WithUser;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Multi-Tenancy tests which testing the CRUD operations of entities that all
 * CRUD-Operations are tenant aware and cannot access or delete entities not
 * belonging to the current tenant.
 *
 *
 *
 *
 */
@Features("Component Tests - Repository")
@Stories("Multi Tenancy")
public class MultiTenancyEntityTest extends AbstractIntegrationTest {

    @Test
    @Description(value = "Ensures that multiple targets with same controller-ID can be created for different tenants.")
    public void createMultipleTargetsWithSameIdForDifferentTenant() throws Exception {
        // known controller ID for overall tenants same
        final String knownControllerId = "controllerId";

        // known tenant names
        final String tenant = "aTenant";
        final String anotherTenant = "anotherTenant";
        // create targets
        createTargetForTenant(knownControllerId, tenant);
        createTargetForTenant(knownControllerId, anotherTenant);

        // ensure both tenants see their target
        final Slice<Target> findTargetsForTenant = findTargetsForTenant(tenant);
        assertThat(findTargetsForTenant).hasSize(1);
        assertThat(findTargetsForTenant.getContent().get(0).getTenant().toUpperCase()).isEqualTo(tenant.toUpperCase());
        final Slice<Target> findTargetsForAnotherTenant = findTargetsForTenant(anotherTenant);
        assertThat(findTargetsForAnotherTenant).hasSize(1);
        assertThat(findTargetsForAnotherTenant.getContent().get(0).getTenant().toUpperCase())
                .isEqualTo(anotherTenant.toUpperCase());

    }

    @Test
    @Description(value = "Ensures that targtes created by a tenant are not visible by another tenant.")
    @WithUser(tenantId = "mytenant", allSpPermissions = true)
    public void queryTargetFromDifferentTenantIsNotVisible() throws Exception {
        // create target for another tenant
        final String anotherTenant = "anotherTenant";
        final String controllerAnotherTenant = "anotherController";
        createTargetForTenant(controllerAnotherTenant, anotherTenant);

        // find all targets for current tenant "mytenant"
        final Slice<Target> findTargetsAll = targetManagement.findTargetsAll(pageReq);
        // no target has been created for "mytenant"
        assertThat(findTargetsAll).hasSize(0);

        // find all targets for anotherTenant
        final Slice<Target> findTargetsForTenant = findTargetsForTenant(anotherTenant);
        // another tenant should have targets
        assertThat(findTargetsForTenant).hasSize(1);
    }

    @Test
    @Description(value = "Ensures that tenant metadata is retrieved for the current tenant.")
    @WithUser(tenantId = "mytenant", autoCreateTenant = false, allSpPermissions = true)
    public void getTenanatMetdata() throws Exception {

        // logged in tenant mytenant - check if tenant default data is
        // autogenerated
        assertThat(distributionSetManagement.findDistributionSetTypesAll(pageReq)).isEmpty();
        assertThat(systemManagement.getTenantMetadata().getTenant().toUpperCase()).isEqualTo("mytenant".toUpperCase());
        assertThat(distributionSetManagement.findDistributionSetTypesAll(pageReq)).isNotEmpty();

        // check that the cache is not getting in the way, i.e. "bumlux" results
        // in bumlux and not
        // mytenant
        assertThat(
                securityRule.runAs(WithSpringAuthorityRule.withUserAndTenant("user", "bumlux"), new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return systemManagement.getTenantMetadata().getTenant().toUpperCase();
                    }
                })).isEqualTo("bumlux".toUpperCase());
    }

    @Test
    @Description(value = "Ensures that targets created from a different tenant cannot be deleted from other tenants")
    @WithUser(tenantId = "mytenant", allSpPermissions = true)
    public void deleteTargetFromOtherTenantIsNotPossible() throws Exception {
        // create target for another tenant
        final String anotherTenant = "anotherTenant";
        final String controllerAnotherTenant = "anotherController";
        final Target createTargetForTenant = createTargetForTenant(controllerAnotherTenant, anotherTenant);

        // ensure target cannot be deleted by 'mytenant'
        targetManagement.deleteTargets(createTargetForTenant.getId());
        Slice<Target> targetsForAnotherTenant = findTargetsForTenant(anotherTenant);
        assertThat(targetsForAnotherTenant).hasSize(1);

        // ensure another tenant can delete the target
        deleteTargetsForTenant(anotherTenant, createTargetForTenant.getId());
        targetsForAnotherTenant = findTargetsForTenant(anotherTenant);
        assertThat(targetsForAnotherTenant).hasSize(0);
    }

    @Test
    @Description(value = "Ensures that multiple distribution sets with same name and version can be created for different tenants.")
    public void createMultipleDistributionSetsWithSameNameForDifferentTenants() throws Exception {

        // known ds name for overall tenants same
        final String knownDistributionSetName = "dsName";
        final String knownDistributionSetVersion = "0.0.0";

        // known tenant names
        final String tenant = "aTenant";
        final String anotherTenant = "anotherTenant";
        // create distribution sets
        createDistributionSetForTenant(knownDistributionSetName, knownDistributionSetVersion, tenant);
        createDistributionSetForTenant(knownDistributionSetName, knownDistributionSetVersion, anotherTenant);

        // ensure both tenants see their distribution sets
        final Page<DistributionSet> findDistributionSetsForTenant = findDistributionSetForTenant(tenant);
        assertThat(findDistributionSetsForTenant).hasSize(1);
        assertThat(findDistributionSetsForTenant.getContent().get(0).getTenant().toUpperCase())
                .isEqualTo(tenant.toUpperCase());
        final Page<DistributionSet> findDistributionSetsForAnotherTenant = findDistributionSetForTenant(anotherTenant);
        assertThat(findDistributionSetsForAnotherTenant).hasSize(1);
        assertThat(findDistributionSetsForAnotherTenant.getContent().get(0).getTenant().toUpperCase())
                .isEqualTo(anotherTenant.toUpperCase());

    }

    private Target createTargetForTenant(final String controllerId, final String tenant) throws Exception {
        return securityRule.runAs(WithSpringAuthorityRule.withUserAndTenant("user", tenant), new Callable<Target>() {
            @Override
            public Target call() throws Exception {
                return targetManagement.createTarget(new Target(controllerId));
            }
        });
    }

    private Slice<Target> findTargetsForTenant(final String tenant) throws Exception {
        return securityRule.runAs(WithSpringAuthorityRule.withUserAndTenant("user", tenant),
                new Callable<Slice<Target>>() {
                    @Override
                    public Slice<Target> call() throws Exception {
                        return targetManagement.findTargetsAll(pageReq);
                    }
                });
    }

    private void deleteTargetsForTenant(final String tenant, final Long... targetIds) throws Exception {
        securityRule.runAs(WithSpringAuthorityRule.withUserAndTenant("user", tenant), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                targetManagement.deleteTargets(targetIds);
                return null;
            }
        });
    }

    private DistributionSet createDistributionSetForTenant(final String name, final String version, final String tenant)
            throws Exception {
        return securityRule.runAs(WithSpringAuthorityRule.withUserAndTenant("user", tenant),
                new Callable<DistributionSet>() {
                    @Override
                    public DistributionSet call() throws Exception {
                        final DistributionSet ds = new DistributionSet();
                        ds.setName(name);
                        ds.setTenant(tenant);
                        ds.setVersion(version);
                        ds.setType(distributionSetManagement
                                .createDistributionSetType(new DistributionSetType("typetest", "test", "foobar")));
                        return distributionSetManagement.createDistributionSet(ds);
                    }
                });
    }

    private Page<DistributionSet> findDistributionSetForTenant(final String tenant) throws Exception {
        return securityRule.runAs(WithSpringAuthorityRule.withUserAndTenant("user", tenant),
                new Callable<Page<DistributionSet>>() {
                    @Override
                    public Page<DistributionSet> call() throws Exception {
                        return distributionSetManagement.findDistributionSetsAll(pageReq, false, false);
                    }
                });
    }

}