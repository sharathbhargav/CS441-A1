package Simulations
import HelperUtils.Common
import HelperUtils.{CreateLogger, UtilityFunctions}
import com.typesafe.config.ConfigFactory
import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
import org.cloudsimplus.builders.tables.CloudletsTableBuilder

import collection.JavaConverters.*

/**
 * This program simulates an Infrastructure as a Service.
 */


class Iaas {

}

object Iaas extends App {
  val config = ConfigFactory.load()
  val logger = CreateLogger(classOf[Iaas])

  val INIT_UTILIZATION_RATIO = config.getDouble("iaas.constant.initUtilizationRation")
  val MAX_UTILIZATION_RATIO = config.getDouble("iaas.constant.maxUtilizationRatio")


  val VM_COUNT = config.getInt("iaas.variable.numberOfVms")
  val CLOUDLET_COUNT = config.getInt("iaas.variable.numberOfCloudLets")


  val BROKER_FIT = config.getString("iaas.variable.brokerFit")


  val COST_CPU = config.getDouble("iaas.constant.cost.cpu")
  val COST_MEM = config.getDouble("iaas.constant.cost.mem")
  val COST_STORAGE = config.getDouble("iaas.constant.cost.storage")
  val COST_BW = config.getDouble("iaas.constant.cost.bw")

  val CLOUD_LEN = config.getInt("serviceResources.cloudLet.length")
  val CLOUD_PE = config.getInt("serviceResources.cloudLet.PEs")

  def Start(hostBin:Int,vmBin:Int,vmScheduling:String,cloudletScheduling:String)={
//        logger.info("iaas start")
    UtilityFunctions.configureLogs()

    //The block name in the application.conf from where the properties have to retreived.
    val configHostPrefix = s"serviceResources.host${hostBin}"
    val configVmPrefix = s"serviceResources.vm${vmBin}"

    val VM_ALLOCATION = config.getString("iaas.variable.vmAllocation")

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
    val CLOUDLET_SCHEDULER = config.getString(s"iaas.variable.cloudletScheduler")

    val HOST_COUNT = config.getInt("iaas.variable.numberOfHosts")
    val VM_COUNT = config.getInt("iaas.variable.numberOfVms")

    val simulation = new CloudSim()
    
    val datacenter = Common.createDataCenter(simulation, HOST_COUNT, HOST_PE, HOST_MIPS, HOST_RAM, HOST_BW, HOST_STORAGE, VM_SCHEDULER,VM_ALLOCATION)
    Common.addCharacteristics(datacenter, COST_CPU, COST_MEM, COST_BW, COST_STORAGE)
    
    val broker0 = Common.createBroker(simulation,BROKER_FIT)
    // create vms 
    val vmList = Common.createVMs(VM_COUNT,VM_MIPS,VM_PE,VM_RAM,VM_BW,VM_STORAGE,CLOUDLET_SCHEDULER)
    // create cloudlets
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
    //    logger.info("iaas end")
  }

  /**
   *  Creates a list of cloudlets with a dynamic utilization ratio. The number of cloudlets are
   *  defined by the variable CLOUDLET_COUNT with length CLOUD_LEN and using CLOUD_PE processin units
   *  The utilization ratio of cloudlet is between INIT_UTILIZATION_RATIO and MAX_UTILIZATION_RATIO and varies dynamically.
   * @return List of CloudletSimple
   */
  def createCloudLets(): List[CloudletSimple] = {
    val utilizationModel: UtilizationModelDynamic = new UtilizationModelDynamic(INIT_UTILIZATION_RATIO)
      .setMaxResourceUtilization(MAX_UTILIZATION_RATIO)
    val cloudletList = List.fill(CLOUDLET_COUNT) {
      new CloudletSimple(CLOUD_LEN, CLOUD_PE, utilizationModel)
    }
    return cloudletList
  }

  logger.info("Running IaaS model")
  logger.info("\n\n\n\nHost config:0, VM config:0, VMs space shared and cloudlets space shared")
  Iaas.Start(0,0,"space","space")
  logger.info("\n\n\n\nHost config:1, VM config:1, VMs time shared and cloudlets space shared")
  Iaas.Start(1,1,"time","space")
  logger.info("\n\n\n\nHost config:2, VM config:2, VMs space shared and cloudlets space shared")
  Iaas.Start(2,2,"space","space")
  logger.info("\n\n\n\nHost config:2, VM config:2, VMs time shared and cloudlets time shared")
  Iaas.Start(2,2,"time","time")
  logger.info("\n\n\n\nHost config:0, VM config:0, VMs time shared and cloudlets time shared")
  Iaas.Start(0,0,"time","time")

}
