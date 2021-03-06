package org.ovirt.engine.core.bll.validator;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.common.ActionUtils;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.errors.EngineMessage;

public class UpgradeHostValidator {

    private VDS host;

    public UpgradeHostValidator(VDS host) {
        this.host = host;
    }

    public ValidationResult hostExists() {
        return ValidationResult.failWith(EngineMessage.VDS_INVALID_SERVER_ID).when(host == null);
    }

    public ValidationResult statusSupportedForHostUpgrade() {
        return ValidationResult.failWith(EngineMessage.CANNOT_UPGRADE_HOST_STATUS_ILLEGAL)
                .unless(ActionUtils.canExecute(Arrays.asList(host), VDS.class, ActionType.UpgradeHost));
    }

    public ValidationResult statusSupportedForHostUpgradeCheck() {
        return ValidationResult.failWith(EngineMessage.CANNOT_CHECK_FOR_HOST_UPGRADE_STATUS_ILLEGAL)
                .unless(ActionUtils.canExecute(Arrays.asList(host), VDS.class, ActionType.HostUpgradeCheck));
    }

    public ValidationResult statusSupportedForHostUpgradeInternal() {
        return ValidationResult.failWith(EngineMessage.CANNOT_UPGRADE_HOST_STATUS_ILLEGAL)
                .when(host.getStatus() != VDSStatus.Maintenance);
    }

    public ValidationResult updatesAvailable() {
        return ValidationResult.failWith(EngineMessage.NO_AVAILABLE_UPDATES_FOR_HOST)
                .unless(host.isOvirtVintageNode() || host.isUpdateAvailable());
    }

    public ValidationResult imageProvidedForOvirtNode(String image) {
        return ValidationResult.failWith(EngineMessage.VDS_CANNOT_INSTALL_MISSING_IMAGE_FILE)
                .when(host.isOvirtVintageNode() && StringUtils.isBlank(image));
    }

    public ValidationResult hostWasInstalled() {
        return ValidationResult.failWith(EngineMessage.CANNOT_UPGRADE_HOST_WITHOUT_OS)
                .when(host.getHostOs() == null);
    }
}
