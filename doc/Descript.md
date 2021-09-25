# Description
##Project Structure
###Simulations
- BasicExample1 - This simulation simulates a basic datacenter with configurable parameters of number of hosts, number of virtual 
machines etc. All configurations can be changed in application.conf file in resources under tag "basicExample1".
- Iaas - This program simulates an Infrastructure as a Service architecture. Further details can found later in this document.
- MultiDataCenter - This program simulates multiple datacenters created across multiple time zones simulating cloudlets being run
in different virtual machines allocated to their closest datacenters, data transfer between datacenters. A network topology is
created to connect different datacenters with broker.
- PaaS - This program simulates Platform as a Service architecture. Further details can found later in this document.
- SaaS - This program simulates Software as a Service architecture. Further details can found later in this document.

###Tests
- A single test file "SimTest".
- 5 tests are present
  - Create a datacenter and test its properties
  - Create a broker and test its properties
  - Create a virtual machine and test its properties
  - Test the functioning of cloudlet creation function
  - Create a host and test its properties

###Configuration parameters
- Only one file "application.conf" is used to store configuration details of all simulations.
- Each of the above simulation has its own configuration block in the file. "commonResources" block contains configuration
of a host machine and virtual machine that is used to create resources in BasicExample1 and MultiDatacenter simulations.
- The block "serviceResources" contains various configurations for host and virtual machines used in all 3 service simulations.
- The three blocks "iaas","paas","saas" consists of configurations used in IaaS, PaaS, SaaS simulation respectively. 
Within these blocks the configuration under "constant" will be defined by the cloud provider and customers will not be able to
change these parameters. The configurations under "variable" block can be configured by customer. In the simulation examples
not all combinations of these parameters is shown.

###Implementation details and results


###SaaS
The cloud provider sets up datacenter, hosts and virtual machine with a defined VM allocation policy at data center, VM
scheduling policy at host level and cloudlet scheduling policy at VM level. In the simulation it is assumed that SaaS
provider will be providing services at different pricing range.
In the simulation there is one data center with 8 hosts and each host having 2 virtual machines.
Host configuration: Depending on the service tier requested by user different host configuration are used
bin 0 : 16 processing units, 16000 MIps, Memory of 16000 MB, Storage 30000 MB, Bndwidth of 30000 MBps
bin 1 : 32 procession units, 32000 MIps, Memory of 32000 MB, Storage 30000 MB, Bndwidth of 30000 MBps
bin 2 : 64 procession units, 64000 MIps, Memory of 64000 MB, Storage 30000 MB, Bndwidth of 30000 MBps
Virtual Machine configuration: To make max use of a host, the virtual machines are fixed as 2 per host, together utilizing
the resources of host completely
Utilization ratio of 0.8 is common across all bins

User can run tasks of any length but can submit burst tasks at maximum of 100 tasks at once.
User can choose from 3 different bins of services providing capablity of processing

Bin 0 : 1 cloudlet/sec -> 10560 + $6.4/sec     cost units
Bin 1 : 2 cloudlets/sec -> 20160 + $12.8/sec   cost units
Bin 2 : 4 cloudlets/sec -> 39360 + $25.6/sec   cost units

The prices shown are indicative and should be considered in realation with other bins.

Results
Cloudlets are of length 3000 and use 1 PE


| Bin   |  Number of Cloudlets  |  Execution time   | Cost incurred | Cost charged to customer  |
|-----  |:-------------:        |------:            |:----:         |:---:                      |
| 0     | 100                   | 803.52            | $10680.528    | $10881.408                |
| 1     | 100                   | 405.11            | $20281.536    | $20484.096                |
| 2     | 100                   | 205.11            | $39483.072    | $39688.192                |

The SaaS designed above is for running large tasks in different times. It is not suited for running lower load and of tasks
as the price does not vary much.
If a SaaS was required at a lower price range for less number of tasks then another host with lower configuration
could be used which would reduce the costs significantly


###PaaS

The cloud provider sets up a datacenter and number of hosts with defined virtual machine allocation policy and virtual
machine scheduling policy. The customer has the option of choosing a number of virtual machines to run their task and
also the option of scheduling mechanism to run the cloudlets within the virtual machine.
Users can choose for 3 different type of virtual machines and create any number of them.

####Configuration
Bin 0: 8 processing units, 6000 MIps, Memory of 6000 MB, Storage 10000 MB, Bndwidth of 10000 MBps
Bin 1: 16 processing units, 12000 MIps, Memory of 12000 MB, Storage 10000 MB, Bndwidth of 10000 MBps
Bin 2: 32 processing units, 24000 MIps, Memory of 24000 MB, Storage 10000 MB, Bndwidth of 10000 MBps

####Cost
Bin 0 : (180 + 0.08* execution time ) * number_of_vms_used   cost units
Bin 1 : (360 + 0.16* execution time ) * number_of_vms_used   cost units
Bin 2 : (720 + 0.32* execution time ) * number_of_vms_used   cost units

| Bin   |  Number of Cloudlets|Number of VMs|Cloudlet Scheduling  |  Execution time   | Cost incurred | Cost charged to customer  |
|:-----  |:-------------:     |:-----------:|  ------:            |:----:             |:---:         | :---:                     |
| 0     | 100                 | 12          | SpaceShared         | 1205.28           | $2196.158    | $2256.4224                |
| 1     | 100                 | 12          | SpaceShared         | 303.84            | $3618.230    | $4368.6144                |
| 1     | 100                 | 12          | TimeShared          | 303.84            | $3618.230    | $4368.6144                |
| 2     | 100                 | 12          | SpaceShared         | 153.83            | $6498.460    | $8689.228                |
| 2     | 100                 | 12          | TimeShared          | 153.83            | $6498.460    | $8689.228                |
| 2     | 1000                | 12          | TimeShared          | 4.43874781108E11  | Too large    | Too large 

###IaaS
IaaS is the bare metal provider service for customers and they can choose whatever configuration of host and virtual
machine they need. In the simulation customer can choose from 3 types of hosts and 3 type of virtual machine, each with
different configuration. It has to be noted that a virtual machine having a higher configuration cannot be run on lower configuration
host. The hosts and virtual machines are numbered 0,1,2 in the increasing order of their configuration.
For this architecture the costs are decided on per cpu processing second, per megabyte of memory, storage and per megabits/second of bandwidth


####Configuration of host available to users
Bin 0: 16 processing units, 16000 MIps, Memory of 16000 MB, Storage 30000 MB, Bndwidth of 30000 MBps
Bin 1: 32 processing units, 32000 MIps, Memory of 32000 MB, Storage 30000 MB, Bndwidth of 30000 MBps
Bin 2: 64 processing units, 64000 MIps, Memory of 64000 MB, Storage 30000 MB, Bndwidth of 30000 MBps

| HostBin| VmBin     |  Number of Cloudlets|Number of Hosts|Number of VMs|Virtual machine Scheduling |Cloudlet Scheduling  |  Execution time   | Cost       |
|:-----: |:-----:    |:-------------:       |:-----------:|:-----------: |: ------:                  |  :------:            |:----:             |:---:      |
| 0      | 0         | 100                 | 10          | 20          | SpaceShared               | SpaceShared         | 1004.4            | $363.013    | 
| 1      | 1         | 100                 | 10          | 20          | TimeShared                | SpaceShared         | 506.3            | $603.038    |
| 2      | 2         | 100                 | 10          | 20          | SpaceShared               | SpaceShared         | 256.3            | $1083.076    |
| 2      | 2         | 1000                | 50          | 100         | SpaceShared               | SpaceShared         | 1282.0           | $5415.383    |
| 2      | 2         | 1000                | 50          | 1000          | TimeShared                | TimeShared          | 1282.0              | $415.383    |

The VM allocation policy has been fixed for the above runs but can be varied by customer in the configuration file under variable parameters
As can be observed when enough resources are provided space shared scheduling and time shared scheduling do no differ much in execution time.


The implementation of FaaS can also be considered similar to SaaS within this simulator since the cloudlet tasks are abstract.
The different bins indicate how fast a user task will be completed by the cloud.


In all of the above architectures, the utilization ratio of 0.8 is considered for virtual machine configuration so that the
host is not overloaded.


###Analysis
For small work loads depicted in the results above there is not much difference between space shared and time shared scheduling
of cloudlets but time shared scheduling causes all cloudlets to be executed at same time and thus takes unusually long time 
due to the internal working of the time shared algorithm. If the load is above a certain degree, the time taken to complete the
task becomes exponentially large as can be seen from the last example of PaaS simulation. The same applies to LinuxFair scheduling.
In this scheduling technique all the cloudlets will not be executed due to how the algorithm is implemented. The simulation 
is terminated once number of cloudlets executed becomes equal to number of VMs when large number of cloudlets are submitted.
Hence none of the simulations shown use LinuxFair scheduling technique.

###Limitations
The customer cannot pick different number of vms of different tiers. This is a requirement in the real world and will take 
a little bit of time to implement in these simulations. Due to time constraints this feature was not implemented.
Custom strategy to assign cloudlets and VMs could be designed to overcome the shortcomings of space and time shared scheduling.