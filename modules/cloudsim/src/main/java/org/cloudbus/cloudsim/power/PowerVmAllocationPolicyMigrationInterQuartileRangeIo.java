/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.power;

import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.util.MathUtil;

/**
 * The Inter Quartile Range (IQR) VM allocation policy.
 * 
 * If you are using any algorithms, policies or workload included in the power package, please cite
 * the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 3.0
 */
public class PowerVmAllocationPolicyMigrationInterQuartileRangeIo extends
		PowerVmAllocationPolicyMigrationAbstractIo {

	/** The safety parameter. */
	private double safetyParameter = 0;

	/** The fallback vm allocation policy. */
	private PowerVmAllocationPolicyMigrationAbstractIo fallbackVmAllocationPolicy;

	/**
	 * Instantiates a new power vm allocation policy migration mad.
	 * 
	 * @param hostList the host list
	 * @param vmSelectionPolicy the vm selection policy
	 * @param safetyParameter the safety parameter
	 * @param utilizationThreshold the utilization threshold
	 * @param vmSelectionPolicyIo 
	 */
	public PowerVmAllocationPolicyMigrationInterQuartileRangeIo(
			List<? extends Host> hostList,
			PowerVmSelectionPolicy vmSelectionPolicy,
			PowerVmSelectionPolicyIo vmSelectionPolicyIo,
			double weightMipsUtil,
			double weightIopsUtil,
			double safetyParameter,
			PowerVmAllocationPolicyMigrationAbstractIo fallbackVmAllocationPolicy,
			double utilizationThreshold) {
		super(hostList, vmSelectionPolicy, vmSelectionPolicyIo, weightMipsUtil, weightIopsUtil);
		setSafetyParameter(safetyParameter);
		setFallbackVmAllocationPolicy(fallbackVmAllocationPolicy);
	}

	/**
	 * Instantiates a new power vm allocation policy migration mad.
	 * 
	 * @param hostList the host list
	 * @param vmSelectionPolicy the vm selection policy
	 * @param safetyParameter the safety parameter
	 */
	public PowerVmAllocationPolicyMigrationInterQuartileRangeIo(
			List<? extends Host> hostList,
			PowerVmSelectionPolicy vmSelectionPolicy,
			PowerVmSelectionPolicyIo vmSelectionPolicyIo,
			double weightMipsUtil,
			double weightIopsUtil,
			double safetyParameter,
			PowerVmAllocationPolicyMigrationAbstractIo fallbackVmAllocationPolicy) {
		super(hostList, vmSelectionPolicy, vmSelectionPolicyIo, weightMipsUtil, weightIopsUtil);
		setSafetyParameter(safetyParameter);
		setFallbackVmAllocationPolicy(fallbackVmAllocationPolicy);
	}

	/**
	 * Checks if is host over utilized.
	 * 
	 * @param _host the _host
	 * @return true, if is host over utilized
	 */
	@Override
	protected boolean isHostOverUtilized(PowerHost host) {
		PowerHostUtilizationHistoryIo _host = (PowerHostUtilizationHistoryIo) host;
		double upperThresholdMips = 0;
		try {
			upperThresholdMips = 1 - getSafetyParameter() * getHostUtilizationMipsIqr(_host);
		} catch (IllegalArgumentException e) {
			return getFallbackVmAllocationPolicy().isHostOverUtilized(host);
		}
		addHistoryEntry(host, upperThresholdMips);
		double totalRequestedMips = 0;
		for (Vm vm : host.getVmList()) {
			totalRequestedMips += vm.getCurrentRequestedTotalMips();
		}
		double mipsUtilization = totalRequestedMips / host.getTotalMips();
		return mipsUtilization > upperThresholdMips;
	}
	
	/**
	 * Checks if is host over utilized.
	 * 
	 * @param _host the _host
	 * @return true, if is host over utilized
	 */
	@Override
	protected boolean isHostOverUtilizedIo(PowerHost host) {
		PowerHostUtilizationHistoryIo _host = (PowerHostUtilizationHistoryIo) host;
		double upperThresholdIops = 0;
		try {
			upperThresholdIops = 1 - getSafetyParameter() * getHostUtilizationIopsIqr(_host);
		} catch (IllegalArgumentException e) {
			return getFallbackVmAllocationPolicy().isHostOverUtilizedIo(host);
		}
		addHistoryEntryIo(host, upperThresholdIops);
		double totalRequestedIops = 0;
		for (Vm vm : host.getVmList()) {
			totalRequestedIops += vm.getCurrentRequestedIops();
		}
		double iopsUtilization = totalRequestedIops / host.getIops();
		return iopsUtilization > upperThresholdIops;
	}
	
	

	/**
	 * Gets the host utilization iqr. (Mips)
	 * 
	 * @param host the host
	 * @return the host utilization iqr
	 */
	protected double getHostUtilizationMipsIqr(PowerHostUtilizationHistory host) throws IllegalArgumentException {
		double[] data = host.getUtilizationHistory();
		if (MathUtil.countNonZeroBeginning(data) >= 12) { // 12 has been suggested as a safe value
			return MathUtil.iqr(data);
		}
		throw new IllegalArgumentException();
	}

	/**
	 * Gets the host utilization iqr. (Iops)
	 * 
	 * @param host the host
	 * @return the host utilization iqr
	 */
	protected double getHostUtilizationIopsIqr(PowerHostUtilizationHistoryIo host) throws IllegalArgumentException {
		double[] data = host.getIoUtilizationHistory();
		//Does the 12 condition hold also for the iops? Who suggested it?
		if (MathUtil.countNonZeroBeginning(data) >= 12) { // 12 has been suggested as a safe value
			return MathUtil.iqr(data);
		}
		throw new IllegalArgumentException();
	}

	/**
	 * Sets the safety parameter.
	 * 
	 * @param safetyParameter the new safety parameter
	 */
	protected void setSafetyParameter(double safetyParameter) {
		if (safetyParameter < 0) {
			Log.printLine("The safety parameter cannot be less than zero. The passed value is: "
					+ safetyParameter);
			System.exit(0);
		}
		this.safetyParameter = safetyParameter;
	}

	/**
	 * Gets the safety parameter.
	 * 
	 * @return the safety parameter
	 */
	protected double getSafetyParameter() {
		return safetyParameter;
	}

	/**
	 * Sets the fallback vm allocation policy.
	 * 
	 * @param fallbackVmAllocationPolicy the new fallback vm allocation policy
	 */
	public void setFallbackVmAllocationPolicy(
			PowerVmAllocationPolicyMigrationAbstractIo fallbackVmAllocationPolicy) {
		this.fallbackVmAllocationPolicy = fallbackVmAllocationPolicy;
	}

	/**
	 * Gets the fallback vm allocation policy.
	 * 
	 * @return the fallback vm allocation policy
	 */
	public PowerVmAllocationPolicyMigrationAbstractIo getFallbackVmAllocationPolicy() {
		return fallbackVmAllocationPolicy;
	}

}
