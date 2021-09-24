### Description

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

Bin 0 : 1 cloudlet/sec -> 10560 + 6.4/sec
Bin 1 : 2 cloudlets/sec -> 20160 + 12.8/sec
Bin 2 : 4 cloudlets/sec -> 39360 + 25.6/sec

The higher capacity services do not differ much from lower tiers when there is low load, the cost difference is noticable
when there are high number of cloudlets in order of ten thousands

Results
Cloudlets are of length 10000 and use 1 PE


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
Users can choose for 3 different type of virtual machines and create any number of them

Bin 0 : (180 + 0.08* execution time ) * number_of_vms_used
Bin 1 : (360 + 0.16* execution time ) * number_of_vms_used
Bin 2 : (720 + 0.32* execution time ) * number_of_vms_used

| Bin   |  Number of Cloudlets|Number of VMs|Cloudlet Scheduling  |  Execution time   | Cost incurred | Cost charged to customer  |
|:-----  |:-------------:     |:-----------:|  ------:            |:----:             |:---:         | :---:                     |
| 0     | 100                 | 12          | SpaceShared         | 1205.28           | $2196.158    | $2256.4224                |
| 1     | 100                 | 12          | SpaceShared         | 303.84            | $3618.230    | $4368.6144                |
| 1     | 100                 | 12          | TimeShared          | 303.84            | $3618.230    | $4368.6144                |
| 2     | 100                 | 12          | SpaceShared         | 153.83            | $6498.460    | $8689.228                |
| 2     | 100                 | 12          | TimeShared          | 153.83            | $6498.460    | $8689.228                |


###IaaS
IaaS is the bare metal provider service for customers and they can choose whatever configuration of host and virtual
machine they need. In the simulation customer can choose from 3 types of hosts and 3 type of virtual machine, each with
different configuration. It has to be noted that a virtual machine having a higher configuration cannot be run on lower configuration
host. The hosts and virtual machines are numbered 0,1,2 in the increasing order of their configuration.

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
