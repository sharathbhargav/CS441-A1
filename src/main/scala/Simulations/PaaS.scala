package Simulations

import HelperUtils.Common
import HelperUtils.{CreateLogger, UtilityFunctions}
import com.typesafe.config.ConfigFactory
import org.cloudbus.cloudsim.brokers.DatacenterBroker
import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
import org.cloudbus.cloudsim.vms.Vm
import org.cloudsimplus.builders.tables.CloudletsTableBuilder

import collection.JavaConverters.*
import collection.convert.ImplicitConversions.*


/**
 * This program simulates Platform as a Service where the customer has freedom over the virtual machines used to run their tasks.
 */
class PaaS {

}

object PaaS extends App {
  val config = ConfigFactory.load()
  val logger = CreateLogger(classOf[PaaS])
  
  val INIT_UTILIZATION_RATIO = config.getDouble("paas.constant.initUtilizationRation")
  val MAX_UTILIZATION_RATIO = config.getDouble("paas.constant.maxUtilizationRatio")


  val DATACENTERS_COUNT = config.getInt("paas.constant.numberOfDataCenters")
  val VM_COUNT = config.getInt("paas.variable.numberOfVms")
  val CLOUDLET_COUNT = config.getInt("paas.variable.numberOfCloudLets")


  val BROKER_FIT = config.getString("paas.constant.brokerFit")
  val VM_ALLOCATION = config.getString("paas.constant.vmAllocation")

  val COST_CPU = config.getDouble("paas.constant.cost.cpu")
  val COST_MEM = config.getDouble("paas.constant.cost.mem")
  val COST_STORAGE = config.getDouble("paas.constant.cost.storage")
  val COST_BW = config.getDouble("paas.constant.cost.bw")

  val BASE_PRICE_0 = config.getDouble("paas.constant.baseCost.base0")
  val BASE_PRICE_1 = config.getDouble("paas.constant.baseCost.base1")
  val BASE_PRICE_2 = config.getDouble("paas.constant.baseCost.base2")

  val BIN0_COST = config.getDouble("paas.constant.baseCost.bin0")
  val BIN1_COST = config.getDouble("paas.constant.baseCost.bin1")
  val BIN2_COST = config.getDouble("paas.constant.baseCost.bin2")

  val CLOUD_LEN = config.getInt("serviceResources.cloudLet.length")
  val CLOUD_PE = config.getInt("serviceResources.cloudLet.PEs")

  def Start(bin:Int,cloudletScheduler:String=config.getString(s"paas.variable.cloudletScheduler"))={
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
    val CLOUDLET_SCHEDULER = cloudletScheduler

    val HOST_COUNT = if(VM_COUNT%2==0) VM_COUNT/2 else 1+ VM_COUNT/2
    val simulation = new CloudSim()
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
    //        new CloudletsTableBuilder(finishedCloudlet).build();

    Common.printCosts(broker0,logger)
    customCost(broker0,bin,VM_COUNT)
  }

  def createCloudLets(): List[CloudletSimple] = {
    val utilizationModel: UtilizationModelDynamic = new UtilizationModelDynamic(INIT_UTILIZATION_RATIO)
    // .setMaxResourceUtilization(MAX_UTILIZATION_RATIO)
    val cloudletList = List.fill(CLOUDLET_COUNT) {
      new CloudletSimple(CLOUD_LEN, CLOUD_PE, utilizationModel)
    }
    return cloudletList
  }
  def customCost(broker: DatacenterBroker,bin:Int,count:Int)={
    val vm = broker.getVmCreatedList[Vm]().maxBy(vm => vm.getTotalExecutionTime())
    val execTime = vm.getTotalExecutionTime
    val totalCost = count * (bin match {
      case 0 => BASE_PRICE_0 + execTime*BIN0_COST
      case 1 => BASE_PRICE_1 + execTime * BIN1_COST
      case 2 => BASE_PRICE_2 + execTime * BIN2_COST
    })
    logger.info(s"Charging customer ${totalCost}")
  }
  logger.info("Running PaaS model")
  logger.info("\n\n\n\nSimulate tier 0 virtual machine with space shared cloudlet scheduling")
  PaaS.Start(0,"space")
  logger.info("\n\n\n\nSimulate tier 1 virtual machine with space shared cloudlet scheduling")
  PaaS.Start(1,"space")
  logger.info("\n\n\n\nSimulate tier 1 virtual machine with time shared cloudlet scheduling")

  PaaS.Start(1,"time")
  logger.info("\n\n\n\nSimulate tier 2 virtual machine with space shared cloudlet scheduling")
  PaaS.Start(2,"space")
  logger.info("\n\n\n\nSimulate tier 2 virtual machine with time shared cloudlet scheduling")
  PaaS.Start(2,"time")
  logger.info("\n\n\n\nEnd of PaaS model")
}
