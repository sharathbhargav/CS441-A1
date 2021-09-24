package Simulations

import HelperUtils.Common.{createBroker, createDataCenter, createVMs}
import com.typesafe.config.{Config, ConfigFactory}
import org.cloudbus.cloudsim.allocationpolicies.{VmAllocationPolicy, VmAllocationPolicyBestFit}
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.Datacenter
import org.cloudbus.cloudsim.schedulers.cloudlet.{CloudletScheduler, CloudletSchedulerSpaceShared}
import org.cloudbus.cloudsim.vms.Vm
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.slf4j.{Logger, LoggerFactory}
import Simulations.Iaas.createCloudLets
import org.cloudbus.cloudsim.cloudlets.CloudletSimple
import Simulations.BasicExample1.createHost
import org.cloudbus.cloudsim.hosts.HostSimple

class SimTest  extends AnyFlatSpec with Matchers    {
  behavior of "Individual elements of simulation"
  val SIM = "application"

  //Initialize Config and Logger objects from 3rd party libraries
  val conf: Config = ConfigFactory.load(SIM+".conf")
  val LOG: Logger = LoggerFactory.getLogger(getClass)
  val simulation = new CloudSim()

  it should "Create a datacenter object and test the properties" in {
    val dc= createDataCenter(simulation,1,1,1000,1024,1000,1000,"space","best")
     dc shouldBe a [Datacenter]
      assert(dc.getHostList.size()==1)
    dc.getVmAllocationPolicy shouldBe a [VmAllocationPolicyBestFit]
  }

  it should "Create a broker and test its properties " in {
    val broker = createBroker(simulation,"simple")
    broker shouldBe a[DatacenterBrokerSimple]

  }

  it should "create vms and test its properties" in {
    val vms = createVMs(2,2000,4,2000,2000,2000,"space")
    vms shouldBe a[List[Vm]]
    val firstVm = vms(0)
    assert(firstVm.getBw.getCapacity==2000)
    assert(firstVm.getRam.getCapacity==2000)
    assert(firstVm.getCloudletScheduler.isInstanceOf[CloudletSchedulerSpaceShared])
  }

  it should "test functioning of Iaas cloudlet creation " in {
    val cloudletList = createCloudLets()
    assert(cloudletList(0).isInstanceOf[CloudletSimple])
  }

  it should "test functioning of basic example host creation "in {
    val host = createHost()
    assert(host.isInstanceOf[HostSimple])
  }

}
