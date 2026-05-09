Vagrant.configure("2") do |config|

  config.vm.box = "ubuntu/jammy64"
  
  config.vm.synced_folder "./shared", "/vagrant_shared", create: true

  nodes = {
    "master" => { ram: 3072, cpu: 2, ip: "192.168.56.10" },
    "datanode1" => { ram: 2048, cpu: 1, ip: "192.168.56.12" },
    "datanode2" => { ram: 2048, cpu: 1, ip: "192.168.56.13" }
  }

  nodes.each do |name, opts|

    config.vm.define name do |node|
      node.vm.hostname = name
      node.vm.network "private_network", ip: opts[:ip]


      node.vm.provider "virtualbox" do |vb|
        vb.name = name
        vb.memory = opts[:ram]
        vb.cpus = opts[:cpu]
      end
    # Port forwarding for Hadoop UIs

      if name == "master"
        node.vm.network "forwarded_port", guest: 9870, host: 9870   # NN UI
        node.vm.network "forwarded_port", guest: 8088, host: 8088   # RM UI
        node.vm.network "forwarded_port", guest: 19888, host: 19888 # JobHistory UI
      end

      if name == "datanode1"
        node.vm.network "forwarded_port", guest: 9864, host: 9864   # DN1 UI
      end
      
      if name == "datanode2"
        node.vm.network "forwarded_port", guest: 9864, host: 9865   # DataNode2 Web UI (different host port)
      end
      
      # Provision after all VMs are created

      if name == "datanode2"
        node.vm.provision "ansible" do |ansible|
          ansible.playbook = "ansible/site.yml"
          ansible.inventory_path = "ansible/inventory.ini"
          ansible.limit = "all"
          ansible.verbose = "v"
        end
      end
    end
  end

end
