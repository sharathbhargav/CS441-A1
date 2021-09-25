package Simulations

import HelperUtils.Common
import com.typesafe.config.ConfigFactory
import HelperUtils.CreateLogger
import HelperUtils.UtilityFunctions
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.brokers.{DatacenterBroker, DatacenterBrokerBestFit, DatacenterBrokerHeuristic, DatacenterBrokerSimple}
import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.{Datacenter, DatacenterSimple}
import org.cloudbus.cloudsim.distributions.UniformDistr
import org.cloudbus.cloudsim.hosts.{Host, HostSimple}
import org.cloudbus.cloudsim.resources.PeSimple
import org.cloudbus.cloudsim.schedulers.cloudlet.{CloudletSchedulerCompletelyFair, CloudletSchedulerSpaceShared, CloudletSchedulerTimeShared}
import org.cloudbus.cloudsim.schedulers.vm.{VmSchedulerSpaceShared, VmSchedulerTimeShared}
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
import org.cloudbus.cloudsim.vms.{Vm, VmCost, VmSimple}
import org.cloudsimplus.autoscaling.{HorizontalVmScaling, HorizontalVmScalingSimple}
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.cloudsimplus.heuristics.CloudletToVmMappingSimulatedAnnealing

import collection.JavaConverters.*
import collection.convert.ImplicitConversions.*

/**
 * This program simulates Software as a Service. The customer can choose from 3 different tiers of service and
 * will pay accordingly.
 */

class SaaS {
}

object SaaS extends App {

  val config = ConfigFactory.load()
  val logger = CreateLogger(classOf[SaaS])

  val INIT_UTILIZATION_RATIO = config.getDouble("saas.constant.initUtilizationRation")
  val MAX_UTILIZATION_RATIO = config.getDouble("saas.constant.maxUtilizationRatio")


  val DATACENTERS_COUNT = config.getInt("saas.constant.numberOfDataCenters")
  val HOST_COUNT = config.getInt("saas.constant.numberOfHosts")
  val VM_COUNT = config.getInt("saas.constant.numberOfVms")
  val CLOUDLET_COUNT = config.getInt("saas.variable.numberOfCloudLets")


  val BROKER_FIT = config.getString("saas.constant.brokerFit")
  val VM_ALLOCATION = config.getString("paas.constant.vmAllocation")

  val COST_CPU = config.getDouble("saas.constant.cost.cpu")
  val COST_MEM = config.getDouble("saas.constant.cost.mem")
  val COST_STORAGE = config.getDouble("saas.constant.cost.storage")
  val COST_BW = config.getDouble("saas.constant.cost.bw")

  val BASE_PRICE_0 = config.getDouble("saas.constant.baseCost.base0")
  val BASE_PRICE_1 = config.getDouble("saas.constant.baseCost.base1")
  val BASE_PRICE_2 = config.getDouble("saas.constant.baseCost.base2")

  val BIN0_COST = config.getDouble("saas.constant.baseCost.bin0")
  val BIN1_COST = config.getDouble("saas.constant.baseCost.bin1")
  val BIN2_COST = config.getDouble("saas.constant.baseCost.bin2")


  val CLOUD_LEN = config.getInt("serviceResources.cloudLet.length")
  val CLOUD_PE = config.getInt("serviceResources.cloudLet.PEs")

  def Start(bin: Int) = {
    UtilityFunctions.configureLogs()
    val configHostPrefix = s"serviceResources.host${bin}"
    val configVmPrefix = s"serviceResources.vm${bin}"

    val HOST_PE = config.getInt(s"${configHostPrefix}.PEs")
    val HOST_MIPS = config.getInt(s"${configHostPrefix}.mipsCapacity")
    val HOST_RAM = config.getInt(s"${configHostPrefix}.RAMInMBs")
    val HOST_STORAGE = config.getInt(s"${configHostPrefix}.StorageInMBs")
    val HOST_BW = config.getInt(s"${configHostPrefix}.BandwidthInMBps")
    val VM_SCHEDULER = config.getString(s"${configHostPrefix}.scheduling")


    val VM_MIPS = config.getInt(s"${configVmPrefix}.mipsCapacity")
    val VM_RAM = config.getInt(s"${configVmPrefix}.RAMInMBs")
    val VM_STORAGE = config.getInt(s"${configVmPrefix}.StorageInMBs")
    val VM_BW = config.getInt(s"${configVmPrefix}.BandwidthInMBps")
    val VM_PE = config.getInt(s"${configVmPrefix}.PEs")
    val CLOUDLET_SCHEDULER = config.getString(s"${configVmPrefix}.scheduling")

    val simulation = new CloudSim()
    logger.info(s"HOST COUNT = ${config.getInt("saas.constant.numberOfHosts")}")
    val datacenter = Common.createDataCenter(simulation, HOST_COUNT, HOST_PE, HOST_MIPS, HOST_RAM, HOST_BW, HOST_STORAGE, VM_SCHEDULER,VM_ALLOCATION)
    Common.addCharacteristics(datacenter, COST_CPU, COST_MEM, COST_BW, COST_STORAGE)

    val broker0 = Common.createBroker(simulation,BROKER_FIT)
    val vmList = Common.createVMs(VM_COUNT,VM_MIPS,VM_PE,VM_RAM,VM_BW,VM_STORAGE,CLOUDLET_SCHEDULER)
    val cloudletList: List[CloudletSimple] = createCloudLets()

    broker0.submitVmList(vmList.asJava)
    broker0.submitCloudletList(cloudletList.asJava)
    simulation.start()

    val finishedCloudlet = broker0.getCloudletFinishedList();
    val cloudletOrdering = Ordering.by { (cloudlet: Cloudlet) =>
      (cloudlet.getId, cloudlet.getExecStartTime)
    }
    finishedCloudlet.sort(cloudletOrdering)

    // Uncomment below line to print individual execution detail about the cloudlets
    //    new CloudletsTableBuilder(finishedCloudlet).build();

    Common.printCosts(broker0,logger)
    customCost(broker0,bin)

  }

  def createCloudLets(): List[CloudletSimple] = {
    val utilizationModel: UtilizationModelDynamic = new UtilizationModelDynamic(INIT_UTILIZATION_RATIO)
      .setMaxResourceUtilization(MAX_UTILIZATION_RATIO)
    val cloudletList = List.fill(CLOUDLET_COUNT) {
      new CloudletSimple(CLOUD_LEN, CLOUD_PE, utilizationModel)
    }
    return cloudletList
  }

  /**
   * Calculates the cost that is presented to customers.
   * @param broker
   * @param bin
   */
  def customCost(broker: DatacenterBroker,bin:Int)={
    val vm = broker.getVmCreatedList[Vm]().maxBy(vm => vm.getTotalExecutionTime())
    val execTime = vm.getTotalExecutionTime
    // base price is the cost of memory, storage and bandwidth combined times number of virtual machines.
    val totalCost = bin match {
      case 0 => BASE_PRICE_0 + execTime*BIN0_COST
      case 1 => BASE_PRICE_1 + execTime * BIN1_COST
      case 2 => BASE_PRICE_2 + execTime * BIN2_COST
    }
    logger.info(s"Charging customer ${totalCost} cost units" )
  }

  logger.info("Running SaaS model")
  logger.info("\n\n\n\nSimulating tier 1: 1 cloudlet/sec service")
  SaaS.Start(0)
  logger.info("\n\n\n\nSimulating tier 2: 2 cloudlet/sec service")
  SaaS.Start(1)
  logger.info("\n\n\n\nSimulating tier 3: 4 cloudlet/sec service")
  SaaS.Start(2)
  logger.info("\n\n\n\nEnd of SaaS model")
}