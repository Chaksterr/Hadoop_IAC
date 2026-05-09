# Hadoop-IAC

> Automated Hadoop 3.3.6 cluster deployment using Infrastructure as Code. Spin up a fully configured 3-node Hadoop cluster with HDFS, YARN, and MapReduce in minutes using Vagrant and Ansible.

## Architecture

```
Master (192.168.56.10)          DataNode1 (192.168.56.12)       DataNode2 (192.168.56.13)
├── NameNode                    ├── DataNode                    ├── DataNode
├── ResourceManager             └── NodeManager                 └── NodeManager
└── JobHistoryServer
```

**Stack:** Hadoop 3.3.6 | Java 11 | Vagrant | Ansible

**Resources:** Master (3GB RAM, 2 CPUs) | DataNodes (2GB RAM, 1 CPU each)

---

## Quick Start

```bash
# Deploy cluster
vagrant up

# Verify
vagrant ssh master
jps  # Should show: NameNode, ResourceManager, JobHistoryServer
```

### Web UIs
- **NameNode:** http://192.168.56.10:9870
- **ResourceManager:** http://192.168.56.10:8088
- **JobHistory:** http://192.168.56.10:19888

---

## Prerequisites

- VirtualBox
- Vagrant
- Ansible
- 7GB+ free RAM

---

## Repository Structure

```
.
├── Vagrantfile                 # VM definitions
├── ansible/
│   ├── site.yml                # Main playbook (setup + start)
│   ├── start_cluster.yml       # Start services
│   ├── stop_cluster.yml        # Stop services
│   ├── inventory.ini           # Node definitions
│   ├── group_vars/
│   │   └── all.yml             # Configuration variables
│   └── roles/
│       ├── common/             # Java, users, SSH
│       ├── hadoop/             # Hadoop installation
│       ├── format_master/      # NameNode formatting
│       ├── initialization/     # Start services
│       └── stopping/           # Stop services
└── shared/                     # Shared files with VMs
```

---

## Managing the Cluster

### Vagrant Commands
```bash
vagrant up              # Start VMs
vagrant halt            # Stop VMs
vagrant destroy -f      # Delete VMs
vagrant ssh master      # SSH to master
vagrant status          # Check status
```

### Ansible Playbooks
```bash
cd ansible

# Full setup
ansible-playbook -i inventory.ini site.yml

# Start/Stop services
ansible-playbook -i inventory.ini start_cluster.yml
ansible-playbook -i inventory.ini stop_cluster.yml
```

### Manual Service Control
```bash
# Check services
jps

# Start services (on master)
sudo -u hadoop /home/hadoop/hadoop/bin/hdfs --daemon start namenode
sudo -u hadoop /home/hadoop/hadoop/bin/yarn --daemon start resourcemanager
sudo -u hadoop /home/hadoop/hadoop/bin/mapred --daemon start historyserver

# Stop services (replace 'start' with 'stop')
```

---

## HDFS Commands

```bash
# List files
hdfs dfs -ls /

# Create directory
hdfs dfs -mkdir -p /user/hadoop/input

# Upload file
hdfs dfs -put file.txt /user/hadoop/input/

# Download file
hdfs dfs -get /user/hadoop/output/result.txt ./

# View file
hdfs dfs -cat /user/hadoop/output/part-00000

# Delete
hdfs dfs -rm -r /user/hadoop/output

# Cluster status
hdfs dfsadmin -report
```

---

## User Management

### Default Users
- **vagrant** - Auto-switches to hadoop on SSH
- **hadoop** - Main service user with sudo

### Add Users

Edit `ansible/group_vars/all.yml`:
```yaml
additional_users:
  - user1
  - analyst
```

Apply changes:
```bash
cd ansible
ansible-playbook -i inventory.ini site.yml
```

Test:
```bash
vagrant ssh master
sudo su - user1
hadoop version
```

---

## Configuration

Edit `ansible/group_vars/all.yml`:
```yaml
hadoop_version: "3.3.6"
java_version: "11"
hadoop_home: "/home/hadoop/hadoop"
namenode_host: "master"
namenode_port: "9000"
```

Configuration templates in `ansible/roles/hadoop/templates/`:
- `core-site.xml.j2`
- `hdfs-site.xml.j2`
- `yarn-site.xml.j2`
- `mapred-site.xml.j2`
- `workers.j2`

---

## Troubleshooting

### Services Not Starting
```bash
# Check logs
tail -f /home/hadoop/hadoop/logs/*.log

# Verify Java
java -version

# Restart services
cd ansible
ansible-playbook -i inventory.ini stop_cluster.yml
ansible-playbook -i inventory.ini start_cluster.yml
```

### NameNode Safe Mode
```bash
hdfs dfsadmin -safemode get
hdfs dfsadmin -safemode leave  # If stuck
```

### DataNodes Not Connecting
```bash
# Check logs
tail -f /home/hadoop/hadoop/logs/hadoop-hadoop-datanode-*.log

# Test connectivity
ping master
ping datanode1
```

### Re-format NameNode (⚠️ Erases all data!)
```bash
cd ansible
ansible-playbook -i inventory.ini stop_cluster.yml

vagrant ssh master
sudo -u hadoop /home/hadoop/hadoop/bin/hdfs namenode -format -force

cd ansible
ansible-playbook -i inventory.ini start_cluster.yml
```
