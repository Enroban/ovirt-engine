package org.ovirt.engine.core.bll.network.macpoolmanager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.math.LongRange;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.errors.EngineError;
import org.ovirt.engine.core.common.errors.EngineException;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.MacAddressRangeUtils;
import org.ovirt.engine.core.utils.lock.AutoCloseableLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacPoolManagerRanges implements MacPoolManagerStrategy {

    private static final Logger log = LoggerFactory.getLogger(MacPoolManagerRanges.class);

    private final ReentrantReadWriteLock lockObj = new ReentrantReadWriteLock();
    private final boolean allowDuplicates;
    private MacsStorage macsStorage;
    private Collection<LongRange> rangesBoundaries;

    public MacPoolManagerRanges(Collection<LongRange> rangesBoundaries, boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
        this.rangesBoundaries = rangesBoundaries;
        initialize();
    }

    private void initialize() {
        log.info("Start initializing {}", getClass().getSimpleName());
        this.macsStorage = createMacsStorage(rangesBoundaries);
        log.info("Finished initializing. Available MACs in pool: {}", macsStorage.getAvailableMacsCount());
    }

    /**
     * create and initialize internal structures to accommodate all macs specified in {@code rangesString} up to {@code
     * maxMacsInPool}.
     *
     * @return initialized {@link MacsStorage} instance.
     */
    private MacsStorage createMacsStorage(Collection<LongRange> rangesBoundaries) {
        MacsStorage macsStorage = new MacsStorage(allowDuplicates);
        for (LongRange range : rangesBoundaries) {
            macsStorage.addRange(range.getMinimumLong(), range.getMaximumLong());
        }

        if (macsStorage.availableMacExist()) {
            return macsStorage;
        } else {
            throw new EngineException(EngineError.MAC_POOL_INITIALIZATION_FAILED);
        }
    }

    private void logWhenMacPoolIsEmpty() {
        if (!macsStorage.availableMacExist()) {
            AuditLogableBase logable = new AuditLogableBase();
            new AuditLogDirector().log(logable, AuditLogType.MAC_POOL_EMPTY);
        }
    }

    @Override
    public String allocateNewMac() {
        try (AutoCloseableLock l = new AutoCloseableLock(lockObj.writeLock())) {
            return allocateNewMacsWithoutLocking(1).get(0);
        }
    }

    private List<String> allocateNewMacsWithoutLocking(int numberOfMacs) {
        List<Long> macs = macsStorage.allocateAvailableMacs(numberOfMacs);
        Collections.sort(macs);
        logWhenMacPoolIsEmpty();

        return MacAddressRangeUtils.macAddressesToStrings(macs);
    }

    @Override
    public int getAvailableMacsCount() {
        try (AutoCloseableLock l = new AutoCloseableLock(lockObj.readLock())) {
            int availableMacsSize = macsStorage.getAvailableMacsCount();
            log.debug("Number of available Mac addresses = {}", availableMacsSize);
            return availableMacsSize;
        }
    }

    @Override
    public void freeMac(String mac) {
        try (AutoCloseableLock l = new AutoCloseableLock(lockObj.writeLock())) {
            macsStorage.freeMac(MacAddressRangeUtils.macToLong(mac));
        }
    }

    @Override
    public boolean addMac(String mac) {
        try (AutoCloseableLock l = new AutoCloseableLock(lockObj.writeLock())) {
            boolean added = macsStorage.useMac(MacAddressRangeUtils.macToLong(mac));
            logWhenMacPoolIsEmpty();
            return added;
        }
    }

    @Override
    public void forceAddMac(String mac) {
        try (AutoCloseableLock l = new AutoCloseableLock(lockObj.writeLock())) {
            forceAddMacWithoutLocking(mac);
        }
    }

    protected void forceAddMacWithoutLocking(String mac) {
        macsStorage.useMacNoDuplicityCheck(MacAddressRangeUtils.macToLong(mac));
        logWhenMacPoolIsEmpty();
    }

    @Override
    public boolean isMacInUse(String mac) {
        try (AutoCloseableLock l = new AutoCloseableLock(lockObj.readLock())) {
            return macsStorage.isMacInUse(MacAddressRangeUtils.macToLong(mac));
        }
    }

    @Override
    public void freeMacs(List<String> macs) {
        try (AutoCloseableLock l = new AutoCloseableLock(lockObj.writeLock())) {
            for (String mac : macs) {
                macsStorage.freeMac(MacAddressRangeUtils.macToLong(mac));
            }
        }
    }

    @Override
    public List<String> allocateMacAddresses(int numberOfAddresses) {
        try (AutoCloseableLock l = new AutoCloseableLock(lockObj.writeLock())) {
            return allocateNewMacsWithoutLocking(numberOfAddresses);
        }
    }

    @Override
    public boolean isDuplicateMacAddressesAllowed() {
        return this.allowDuplicates;
    }
}
