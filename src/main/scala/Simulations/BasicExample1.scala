package Simulations

import com.typesafe.config.ConfigFactory
import HelperUtils.CreateLogger
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.brokers.{DatacenterBroker, DatacenterBrokerBestFit, DatacenterBrokerHeuristic, DatacenterBrokerSimple}
import org.cloudbus.cloudsim.cloudlets.{Cloudlet, CloudletSimple}
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.DatacenterSimple
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

/**
 * This example consists of concepts of:
 * --Basic cloudlet execution
 * --Delayed simulation termination. The simulator waits for cloudlets until a set defined time
 * --Delayed submission of cloudlets using random time. This can be substituted with any distribution.
 * --Basic cost calculation
 */
class BasicExample1 {
}

object BasicExample1 extends App {
  val config = ConfigFactory.load()
  val logger = CreateLogger(classOf[BasicExample1])

  val INIT_UTILIZATION_RATIO = config.getDouble("basicExample1.initUtilizationRation")
  val MAX_UTILIZATION_RATIO = config.getDouble("basicExample1.maxUtilizationRatio")

  val SIM_TIME = config.getDouble("basicExample1.simulationTime")
  val DATACENTERS_COUNT = config.getInt("basicExample1.numberOfDataCenters")
  val HOST_COUNT = config.getInt("basicExample1.numberOfHosts")
  val VM_COUNT = config.getInt("basicExample1.numberOfVms")
  val CLOUDLET_COUNT = config.getInt("basicExample1.numberOfCloudLets")


  val BROKER_FIT = config.getString("basicExample1.brokerFit")


  val HOST_PE = config.getInt("commonResources.host.PEs")
  val HOST_MIPS = config.getInt("commonResources.host.mipsCapacity")
  val HOST_RAM = config.getInt("commonResources.host.RAMInMBs")
  val HOST_STORAGE = config.getInt("commonResources.host.StorageInMBs")
  val HOST_BW = config.getInt("commonResources.host.BandwidthInMBps")
  val VM_SCHEDULER = config.getString("commonResources.host.scheduling")


  val VM_MIPS = config.getInt("commonResources.vm.mipsCapacity")
  val VM_RAM = config.getInt("commonResources.vm.RAMInMBs")
  val VM_STORAGE = config.getInt("commonResources.vm.StorageInMBs")
  val VM_BW = config.getInt("commonResources.vm.BandwidthInMBps")
  val VM_PE = config.getInt("commonResources.vm.PEs")
  val CLOUDLET_SCHEDULER = config.getString("commonResources.vm.scheduling")


  val CLOUD_LEN = config.getInt("basicExample1.cloudLet.length")
  val CLOUD_PE = config.getInt("basicExample1.cloudLet.PEs")


  val COST_CPU = config.getDouble("basicExample1.cost.cpu")
  val COST_MEM = config.getDouble("basicExample1.cost.mem")
  val COST_STORAGE = config.getDouble("basicExample1.cost.storage")
  val COST_BW = config.getDouble("basicExample1.cost.bw")

  Start()
  def Start() = {


    val simulation = new CloudSim()
    val datacenter = createDataCenter(simulation)
    val broker0 = createBroker(simulation)
    val vmList = createVMs()
    val cloudletList: List[CloudletSimple] = createCloudLets()
    simulation.terminateAt(SIM_TIME)
    broker0.submitVmList(vmList.asJava)
    broker0.submitCloudletList(cloudletList.asJava)
    simulation.start()

    val finishedCloudlet = broker0.getCloudletFinishedList();
    val cloudletOrdering = Ordering.by { (cloudlet: Cloudlet) =>
      (cloudlet.getId, cloudlet.getExecStartTime)
    }
    finishedCloudlet.sort(cloudletOrdering)
    new CloudletsTableBuilder(finishedCloudlet).build();

    printCosts(broker0)
  }

  def createDataCenter(sim: CloudSim): DatacenterSimple = {
    val hostList = List.fill(HOST_COUNT) {
      createHost()
    }
    val datacenterSimple = new DatacenterSimple(sim, hostList.asJava)
    datacenterSimple.getCharacteristics
      .setCostPerSecond(COST_CPU)
      .setCostPerMem(COST_MEM)
      .setCostPerStorage(COST_STORAGE)
      .setCostPerBw(COST_BW)
    return datacenterSimple
  }

  def createHost(): Host = {
    val pe = List.fill(HOST_PE) {
      new PeSimple(HOST_MIPS)
    }
    return new HostSimple(HOST_RAM,
      HOST_BW,
      HOST_STORAGE,
      (pe).asJava).setVmScheduler(VM_SCHEDULER match {
      case "space" => new VmSchedulerSpaceShared()
      case "time" => new VmSchedulerTimeShared()
    })
  }

  def createBroker(sim: CloudSim): DatacenterBroker = {
    val broker = BROKER_FIT match {
      case "simple" => new DatacenterBrokerSimple(sim)
      case "best" => new DatacenterBrokerBestFit(sim)
      case "annealing" => {
        val SA_INITIAL_TEMPERATURE = 1.0;
        val SA_COLD_TEMPERATURE = 0.0001;
        val SA_COOLING_RATE = 0.003;
        val SA_NUMBER_OF_NEIGHBORHOOD_SEARCHES = 50;
        val heuristic = new CloudletToVmMappingSimulatedAnnealing(SA_INITIAL_TEMPERATURE, new UniformDistr(0, 1));
        heuristic.setColdTemperature(SA_COLD_TEMPERATURE)
        heuristic.setCoolingRate(SA_COOLING_RATE)
        heuristic.setNeighborhoodSearchesByIteration(SA_NUMBER_OF_NEIGHBORHOOD_SEARCHES)
        val broker = new DatacenterBrokerHeuristic(sim)
        broker.setHeuristic(heuristic)
        broker
      }
    }
    return broker
  }

  def createVMs(): List[Vm] = {
    val vmList = List.fill(VM_COUNT) {
      val vm = new VmSimple(VM_MIPS, VM_PE)
        .setCloudletScheduler(CLOUDLET_SCHEDULER match {
          case "space" => new CloudletSchedulerSpaceShared()
          case "time" => new CloudletSchedulerTimeShared()
          case "fair" => new CloudletSchedulerCompletelyFair()
        })
      vm.setRam(VM_RAM).setBw(VM_BW).setSize(VM_STORAGE)
      vm
    }
    return vmList
  }

  def createCloudLets(): List[CloudletSimple] = {
    val utilizationModel: UtilizationModelDynamic = new UtilizationModelDynamic(INIT_UTILIZATION_RATIO)
      .setMaxResourceUtilization(MAX_UTILIZATION_RATIO)
    val cloudletList = List.fill(CLOUDLET_COUNT) {
      val r = scala.util.Random
      val cloudlet = CloudletSimple(CLOUD_LEN, CLOUD_PE, utilizationModel)
      cloudlet.setSubmissionDelay(r.nextInt(SIM_TIME.toInt))
      cloudlet
    }
    return cloudletList
  }

  def printCosts(broker: DatacenterBroker) = {
    var totalCost = 0.0
    var totalNonIdleVms = 0
    var processingTotalCost = 0.0
    var memoryTotaCost: Double = 0
    var storageTotalCost: Double = 0
    var bwTotalCost: Double = 0
    var totalExecutionTime: Double = 0
    broker.getVmCreatedList.forEach({ vm =>
      val cost: VmCost = new VmCost(vm)
      processingTotalCost = processingTotalCost + cost.getProcessingCost
      memoryTotaCost = memoryTotaCost + cost.getMemoryCost
      storageTotalCost = storageTotalCost + cost.getStorageCost
      bwTotalCost = bwTotalCost + cost.getBwCost
      totalCost = totalCost + cost.getTotalCost
      totalExecutionTime = totalExecutionTime + cost.getVm.getTotalExecutionTime
      totalNonIdleVms = totalNonIdleVms + (if (vm.getTotalExecutionTime > 0) 1 else 0)
    })
    logger.info(s"Total execution time = ${totalExecutionTime}")
    logger.info(s"Total cost = ${totalCost}")
    logger.info(s"Total non idle vms = ${totalNonIdleVms}")
    logger.info(s"Total processing cost = ${processingTotalCost}")
    logger.info(s"Total memory cost = ${memoryTotaCost}")
    logger.info(s"Total BW cost = ${bwTotalCost}")
    logger.info(s"Total storage cost = ${storageTotalCost}")
  }
}