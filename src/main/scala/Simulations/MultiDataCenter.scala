package Simulations

import HelperUtils.{CreateLogger}
import com.typesafe.config.ConfigFactory
import org.cloudbus.cloudsim.allocationpolicies.{VmAllocationPolicyBestFit, VmAllocationPolicyRoundRobin, VmAllocationPolicySimple}
import org.cloudbus.cloudsim.brokers.{DatacenterBroker, DatacenterBrokerBestFit, DatacenterBrokerHeuristic, DatacenterBrokerSimple}
import org.cloudbus.cloudsim.cloudlets.CloudletSimple
import org.cloudbus.cloudsim.cloudlets.network.{CloudletExecutionTask, CloudletReceiveTask, CloudletSendTask, NetworkCloudlet}
import org.cloudbus.cloudsim.core.{CloudSim, SimEntity}
import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter
import org.cloudbus.cloudsim.distributions.UniformDistr
import org.cloudbus.cloudsim.hosts.{Host, network}
import org.cloudbus.cloudsim.hosts.network.NetworkHost
import org.cloudbus.cloudsim.network.switches.EdgeSwitch
import org.cloudbus.cloudsim.network.topologies.{BriteNetworkTopology, NetworkTopology}
import org.cloudbus.cloudsim.resources.PeSimple
import org.cloudbus.cloudsim.schedulers.cloudlet.{CloudletSchedulerCompletelyFair, CloudletSchedulerSpaceShared, CloudletSchedulerTimeShared}
import org.cloudbus.cloudsim.schedulers.vm.{VmSchedulerSpaceShared, VmSchedulerTimeShared}
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic
import org.cloudbus.cloudsim.vms.VmCost
import org.cloudbus.cloudsim.vms.network.NetworkVm
import org.cloudsimplus.builders.tables.CloudletsTableBuilder
import org.cloudsimplus.heuristics.CloudletToVmMappingSimulatedAnnealing
import org.cloudsimplus.listeners.EventInfo

import scala.collection.immutable.TreeMap
import collection.JavaConverters.*
import scala.collection.mutable.ListBuffer
import scala.util.Random
import collection.JavaConverters.*
import collection.convert.ImplicitConversions.*

/**
 * This program simulates creation of multiple datacenters across different time zones. 
 * It also simulates creation of a network topology by creating links between each datacenter and broker.
 * Edge switches are created within each datacenter to connect the hosts.
 */

class MultiDataCenter {
}

object MultiDataCenter  extends App{
  val config = ConfigFactory.load()
  val logger = CreateLogger(classOf[MultiDataCenter])

  val SIMULATION_TIME = config.getInt("multiDataCenter.simulationTime")
  val INIT_UTILIZATION_RATIO = config.getDouble("multiDataCenter.initUtilizationRation")
  val MAX_UTILIZATION_RATIO = config.getDouble("multiDataCenter.maxUtilizationRatio")

  val TIME_ZONES = config.getDoubleList("multiDataCenter.timeZones")
  val NETWORK_LATENCY_TO_BROKER = config.getDoubleList("multiDataCenter.networkLatencyToBroker")
  val NETWORK_BANDWIDTH_DC = config.getInt("multiDataCenter.networkBwBetweenDc")
  val NETWORK_LATENCY_BETWEEN_DC = config.getInt("multiDataCenter.networkLatencyBetweenDC")


  val DATACENTERS_COUNT = config.getInt("multiDataCenter.numberOfDataCenters")
  val HOST_COUNT_PER_DC = config.getInt("multiDataCenter.numberOfHostsPerDC")
  val VM_COUNT_PER_HOST = config.getInt("multiDataCenter.numberOfVmsPerHost")
  val CLOUDLET_COUNT = config.getInt("multiDataCenter.numberOfCloudLets")
  val SCHEDULING_INTERVAL = config.getDouble("multiDataCenter.schedulingInterval")

  val BROKER_FIT = config.getString("multiDataCenter.brokerFit")
  val VM_ALLOCATION_POLICY = config.getString("multiDataCenter.vmAllocation")

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


  val CLOUD_LEN = config.getInt("multiDataCenter.cloudLet.length")
  val CLOUD_PE = config.getInt("multiDataCenter.cloudLet.PEs")
  val CLOUD_MEM = config.getInt("multiDataCenter.cloudLet.mem")
  val CLOUD_IN_SIZE = config.getInt("multiDataCenter.cloudLet.input")
  val CLOUD_OUT_SIZE = config.getInt("multiDataCenter.cloudLet.output")

  val COST_CPU = config.getDouble("multiDataCenter.cost.cpu")
  val COST_MEM = config.getDouble("multiDataCenter.cost.mem")
  val COST_STORAGE = config.getDouble("multiDataCenter.cost.storage")
  val COST_BW = config.getDouble("multiDataCenter.cost.bw")

  Start()
  def Start() = {
//    UtilityFunctions.configureLogs()
    val simulation = new CloudSim()
    val broker0 = createBroker(simulation)
    broker0.setSelectClosestDatacenter(true)
    val datacenterList = createDatacenters(simulation)
    configureNetwork(simulation, datacenterList, broker0)
    val vmList = createVMsAndSubmit(broker0)
    createCloudletsAndSubmit(broker0,vmList)
    broker0.submitCloudletList(createCloudLets())
    simulation.terminateAt(SIMULATION_TIME)
    simulation.start()

    val finishedCloudlet = broker0.getCloudletFinishedList();
    new CloudletsTableBuilder(finishedCloudlet).build();

    printCosts(broker0)
  }

  /**
   * Creates broker depending on the BROKER_FIT configuration parameter. 
   * @param sim
   * @return DatacenterBroker
   */
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

  def createDatacenters(sim: CloudSim): List[NetworkDatacenter] = {

    val datacenterList = TIME_ZONES.map(timeZone => {
      val hostList = List.fill(HOST_COUNT_PER_DC) {
        createHost()
      }
      val dc = new NetworkDatacenter(sim, hostList.asJava, VM_ALLOCATION_POLICY match {
        case "best" => new VmAllocationPolicyBestFit()
        case "round" => new VmAllocationPolicyRoundRobin()
        case "simple" => new VmAllocationPolicySimple()
        case default => new VmAllocationPolicySimple()
      })
      dc.setTimeZone(timeZone)
      dc.setSchedulingInterval(SCHEDULING_INTERVAL)
      addCharacteristics(dc)
      createNetwork(dc, sim)
      dc.asInstanceOf[NetworkDatacenter]
    }).toList
    return datacenterList
  }

  def createHost(): Host = {
    val pe = List.fill(HOST_PE) {
      new PeSimple(HOST_MIPS)
    }
    return new NetworkHost(HOST_RAM,
      HOST_BW,
      HOST_STORAGE,
      (pe).asJava).setVmScheduler(VM_SCHEDULER match {
      case "space" => new VmSchedulerSpaceShared()
      case "time" => new VmSchedulerTimeShared()
    })
  }

  def addCharacteristics(dc: NetworkDatacenter) = {
    dc.getCharacteristics
      .setCostPerSecond(COST_CPU)
      .setCostPerMem(COST_MEM)
      .setCostPerStorage(COST_STORAGE)
      .setCostPerBw(COST_BW)
  }

  def createNetwork(dc: NetworkDatacenter, simulation: CloudSim): Unit = {
    val edgeSwitches = List.fill(1) {
      val edgeSwitch = new EdgeSwitch(simulation, dc)
      dc.addSwitch(edgeSwitch)
      edgeSwitch
    }
    val hostList = dc.getHostList[NetworkHost]
    hostList.forEach(host => {
      val switchNum = getSwitchIndex(host, edgeSwitches(0).getPorts)
      edgeSwitches(switchNum).connectHost(host)
    })
  }

  def getSwitchIndex(host: NetworkHost, switchPorts: Int): Int = Math.round(host.getId % Integer.MAX_VALUE) / switchPorts

  def configureNetwork(simulation: CloudSim, datacenterList: List[NetworkDatacenter], broker0: DatacenterBroker): Unit = {
    val networkTopology = new BriteNetworkTopology()
    simulation.setNetworkTopology(networkTopology)
    // Add link between each datacenter and broker
    datacenterList.zipWithIndex.map { case (dc, index) => {
      networkTopology.addLink(dc, broker0, NETWORK_BANDWIDTH_DC, NETWORK_LATENCY_TO_BROKER(index))
    }
    }
    //Add link between the 4 datacenters
    networkTopology.addLink(datacenterList(0), datacenterList(1), NETWORK_BANDWIDTH_DC, NETWORK_LATENCY_BETWEEN_DC)
    networkTopology.addLink(datacenterList(0), datacenterList(2), NETWORK_BANDWIDTH_DC, NETWORK_LATENCY_BETWEEN_DC)
    networkTopology.addLink(datacenterList(0), datacenterList(3), NETWORK_BANDWIDTH_DC, NETWORK_LATENCY_BETWEEN_DC)
    networkTopology.addLink(datacenterList(2), datacenterList(3), NETWORK_BANDWIDTH_DC, NETWORK_LATENCY_BETWEEN_DC)
  }

  def createVMsAndSubmit(broker:DatacenterBroker): List[NetworkVm] = {
    var vmList = new ListBuffer[NetworkVm]()
    TIME_ZONES.map(timezone => {
      val vms = List.tabulate(VM_COUNT_PER_HOST*HOST_COUNT_PER_DC) { n =>
        val vm = new NetworkVm(VM_MIPS, VM_PE).setCloudletScheduler(CLOUDLET_SCHEDULER match {
          case "space" => new CloudletSchedulerSpaceShared()
          case "time" => new CloudletSchedulerTimeShared()
          case "fair" => new CloudletSchedulerCompletelyFair()
        })
        vm.setRam(VM_RAM).setBw(VM_BW)
        vm.setTimeZone(timezone)

        vmList += vm.asInstanceOf[NetworkVm]
        vm
      }
      logger.info(s"In create vms. finished creating ${vms.size} vms for datacenter ${timezone} datacenter")
      broker.submitVmList(vms.asJava)
    })
    vmList.toList
  }

  def createCloudletsAndSubmit(broker:DatacenterBroker,vmList:List[NetworkVm]) ={
    val utilizationModel: UtilizationModelDynamic = new UtilizationModelDynamic(INIT_UTILIZATION_RATIO)
      .setMaxResourceUtilization(MAX_UTILIZATION_RATIO)
    val cloudletList = List.tabulate(CLOUDLET_COUNT) {n=>
      val cloudlet : CloudletSimple = new CloudletSimple(CLOUD_LEN, CLOUD_PE)
      cloudlet.setUtilizationModel(utilizationModel)
      cloudlet.setFileSize(CLOUD_IN_SIZE).setOutputSize(CLOUD_OUT_SIZE)
      val selectedVm = vmList.get(n%vmList.size)
      cloudlet.setVm(selectedVm)
      cloudlet.setBroker(selectedVm.getBroker)
      cloudlet
    }

    val cloudletListSize = cloudletList.size
    /*
    cloudletList.zipWithIndex.map((cloudlet,i) =>{

      if(i%2==0){
        addExecutionTask(cloudletList.get(i))
        val next = if(i+1<cloudletListSize) i+1 else 0
        addSendTask(cloudlet,cloudletList(next))
      }
      else {
        val prev = if(i-1>0) i-1 else 0
        addReceiveTask(cloudletList.get(i-1),cloudlet)
        addExecutionTask(cloudlet)
      }
    })*/
    broker.submitCloudletList(cloudletList)
  }

  def createCloudLets(): List[CloudletSimple] = {
    logger.info("Creating normal cloudlet")
    val utilizationModel: UtilizationModelDynamic = new UtilizationModelDynamic(INIT_UTILIZATION_RATIO)
      .setMaxResourceUtilization(MAX_UTILIZATION_RATIO)
    val cloudletList = List.fill(CLOUDLET_COUNT) {
      val r = scala.util.Random
      val cloudlet = CloudletSimple(CLOUD_LEN, CLOUD_PE, utilizationModel)
      cloudlet
    }
    return cloudletList
  }
  def addExecutionTask(networkCloudlet: NetworkCloudlet) = {
    val cloudletTask = new CloudletExecutionTask(networkCloudlet.getTasks.size(), networkCloudlet.getLength)
    cloudletTask.setMemory(networkCloudlet.getMemory)
    networkCloudlet.addTask(cloudletTask)
  }

  def addSendTask(sourceCloudlet: NetworkCloudlet, destinationCloudlet: NetworkCloudlet) = {
    val task = new CloudletSendTask(sourceCloudlet.getTasks.size);
    task.setMemory(sourceCloudlet.getMemory);
    sourceCloudlet.addTask(task);
    for ( i <- 1 to 100)
    {
      task.addPacket(destinationCloudlet, 1000);
    }
  }

  def addReceiveTask(cloudlet: NetworkCloudlet,sourceCloudlet : NetworkCloudlet)={
    val cloudeletReceiveTask = new CloudletReceiveTask(cloudlet.getTasks.size(),sourceCloudlet.getVm)
    cloudeletReceiveTask.setMemory(100)
    cloudeletReceiveTask.setExpectedPacketsToReceive(100)
    cloudlet.addTask(cloudeletReceiveTask)
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
      totalExecutionTime = totalExecutionTime+ cost.getVm.getTotalExecutionTime
      totalNonIdleVms = totalNonIdleVms + (if (vm.getTotalExecutionTime > 0) 1 else 0)
    })
    logger.info(s"Total execution time = ${totalExecutionTime} seconds")
    logger.info(s"Total cost = ${totalCost} cost units")
    logger.info(s"Total non idle vms = ${totalNonIdleVms} cost units")
    logger.info(s"Total processing cost = ${processingTotalCost} cost units")
    logger.info(s"Total memory cost = ${memoryTotaCost} cost units")
    logger.info(s"Total BW cost = ${bwTotalCost} cost units")
    logger.info(s"Total storage cost = ${storageTotalCost} cost units")
  }

}
