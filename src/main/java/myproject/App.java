package myproject;


import com.pulumi.aws.AwsFunctions;
import com.pulumi.aws.Config;
import com.pulumi.Pulumi;
import com.pulumi.Context;
import com.pulumi.aws.ec2.Instance;
import com.pulumi.aws.ec2.InstanceArgs;
import com.pulumi.aws.ec2.InternetGateway;
import com.pulumi.aws.ec2.InternetGatewayArgs;
import com.pulumi.aws.ec2.Route;
import com.pulumi.aws.ec2.RouteArgs;
import com.pulumi.aws.ec2.RouteTable;
import com.pulumi.aws.ec2.RouteTableArgs;
import com.pulumi.aws.ec2.RouteTableAssociation;
import com.pulumi.aws.ec2.RouteTableAssociationArgs;
import com.pulumi.aws.ec2.SecurityGroup;
import com.pulumi.aws.ec2.SecurityGroupArgs;
import com.pulumi.aws.ec2.Subnet;
import com.pulumi.aws.ec2.SubnetArgs;
import com.pulumi.aws.ec2.Vpc;
import com.pulumi.aws.inputs.GetAvailabilityZonesArgs;
import com.pulumi.aws.opsworks.RdsDbInstance;
import com.pulumi.aws.opsworks.RdsDbInstanceArgs;
import com.pulumi.aws.rds.Cluster;
import com.pulumi.aws.rds.ClusterArgs;
import com.pulumi.aws.rds.ParameterGroup;
import com.pulumi.aws.rds.ParameterGroupArgs;
import com.pulumi.aws.route53.Record;
import com.pulumi.aws.route53.RecordArgs;
import com.pulumi.aws.route53.Zone;
import com.pulumi.aws.route53.ZoneArgs;
import com.pulumi.core.Output;


import pulumirpc.Provider.CreateRequest;

import com.pulumi.aws.s3.Bucket;
import com.pulumi.aws.servicediscovery.PublicDnsNamespace;
import com.pulumi.aws.ec2.SubnetArgs;
import com.pulumi.aws.ec2.VpcArgs;
import com.pulumi.aws.ec2.enums.InstanceType;
import com.pulumi.aws.ec2.inputs.SecurityGroupEgressArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs;
import com.pulumi.aws.ec2.outputs.SecurityGroupIngress;
import com.pulumi.aws.iam.InstanceProfile;
import com.pulumi.aws.iam.InstanceProfileArgs;
import com.pulumi.aws.iam.Role;
import com.pulumi.aws.iam.RoleArgs;
import com.pulumi.aws.iam.RolePolicyAttachment;
import com.pulumi.aws.iam.RolePolicyAttachmentArgs;
import com.pulumi.aws.Provider;
import com.pulumi.aws.ProviderArgs;
import com.pulumi.aws.docdb.SubnetGroup;
import com.pulumi.aws.docdb.SubnetGroupArgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class App {
    public static void main(String[] args) {

        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx){
        // Define parameters such as AWS region and CIDR blocks
        String awsRegion = System.getenv("PULUMI_CONFIG_AWS_REGION");
        String vpcCidrBlock = System.getenv("PULUMI_CONFIG_VPC_CIDR_BLOCK");

        

        // Create the VPC
        var vpc = new Vpc("MyVPC", VpcArgs.builder()
            .cidrBlock(vpcCidrBlock)
            .enableDnsSupport(true)
            .enableDnsHostnames(true)
            .tags(Map.of("Name", "MyVPC"))
            .build());

        if (vpc == null) {
                System.err.println("Failed to create VPC. Check your configuration.");
                return;
        }

        

        // Create public and private route tables
        RouteTable publicRouteTable = new RouteTable("PublicRouteTable", RouteTableArgs.builder()
            .vpcId(vpc.id())
            .build());

        RouteTable privateRouteTable = new RouteTable("PrivateRouteTable", RouteTableArgs.builder()
            .vpcId(vpc.id())
            .build());

        InternetGateway internetGateway = new InternetGateway("MyInternetGateway", InternetGatewayArgs.builder()
            .vpcId(vpc.id())
            .build());
        
        final var available = AwsFunctions.getAvailabilityZones(GetAvailabilityZonesArgs.builder()
            .state("available")
            .build());
        
        int size = Arrays.asList(available).size();
        List<?> a = Arrays.asList(available);
        List<Subnet> dbSubnetGroup = new ArrayList<>();
        List<Subnet> publicSub = new ArrayList<>();
        // Create public and private subnets
        for (int i = 1; i <= 3; i++) {
            String az = String.format("%s%s", awsRegion, (char) (i + 96));

            String publicCidrBlock = String.format("10.0.%d.0/24", i);
            String privateCidrBlock = String.format("10.0.%d.0/24", i+3);
            final int t = i;
            Subnet publicSubnet = new Subnet("PublicSubnet"+ i, SubnetArgs.builder()
                .availabilityZone(available.applyValue(getAvailabilityZonesResult -> getAvailabilityZonesResult.names().get(t)))
                .cidrBlock(publicCidrBlock)
                .vpcId(vpc.id())
                .mapPublicIpOnLaunch(true)
                
                .build());
            publicSub.add(publicSubnet);

            var routeTableAssociationArgsPub = new RouteTableAssociation("RouteTableAssociationPub"+i, RouteTableAssociationArgs.builder().routeTableId(publicRouteTable.id()).subnetId(publicSubnet.id()).build());
            //RouteTableAssociation routeTableAssociationPub = new RouteTableAssociation("publicRouteTableAssociationPub" + i, routeTableAssociationArgsPub);

            Subnet privateSubnet = new Subnet("PrivateSubnet" + i, SubnetArgs.builder()
                .availabilityZone(available.applyValue(getAvailabilityZonesResult -> getAvailabilityZonesResult.names().get(t)))                
                .cidrBlock(privateCidrBlock)
                .vpcId(vpc.id())
                .build());
            
            dbSubnetGroup.add(privateSubnet);

            var routeTableAssociationArgsPriv = new RouteTableAssociation("RouteTableAssociationPriv"+i, RouteTableAssociationArgs.builder().routeTableId(privateRouteTable.id()).subnetId(privateSubnet.id()).build());
            //RouteTableAssociation routeTableAssociationPriv = new RouteTableAssociation("publicRouteTableAssociationPriv" + i, routeTableAssociationArgsPriv);

        }

        List<Output<String>> subnetIds = new ArrayList<>();
        for(Subnet s : dbSubnetGroup){
            subnetIds.add(s.id());
        }

        Output<List<String>> subnetIdsOutput = Output.all(subnetIds).applyValue(ids -> ids);

        SubnetGroup subnetGroup = new SubnetGroup("private-subnet-group", SubnetGroupArgs.builder()
            .subnetIds(subnetIdsOutput)
            .description("subnet to access database")
            .build()
        );

        // Create an Internet Gateway and attach it to the VPC
        // InternetGateway internetGateway = new InternetGateway("MyInternetGateway", InternetGatewayArgs.builder()
        //     .vpcId(vpc.id())
        //     .build());

        // // Create public and private route tables
        // RouteTable publicRouteTable = new RouteTable("PublicRouteTable", RouteTableArgs.builder()
        //     .vpcId(vpc.id())
        //     .build());

        // RouteTable privateRouteTable = new RouteTable("PrivateRouteTable", RouteTableArgs.builder()
        //     .vpcId(vpc.id())
        //     .build());

        // Attach public and private subnets to their respective route tables
        // for (int i = 1; i <= 3; i++) {
        //     String az = String.format("%s%s", awsRegion, (char) (i + 96));
        //     // Subnet publicSubnet = Subnet.get("PublicSubnet" + i, i);
        //     // Subnet privateSubnet = Subnet.get("PrivateSubnet" + i, i);

        //     // publicSubnet.setRouteTableId(publicRouteTable.id());
        //     // privateSubnet.setRouteTableId(privateRouteTable.id());

        //     // Subnet publicSubnet = Subnet.get("PublicSubnet" + i, SubnetArgs.builder()
        //     //     .availabilityZone(az)
        //     //     .vpcId(vpc.id())
        //     //     .build());

        //     // Subnet privateSubnet = Subnet.get("PrivateSubnet" + i, SubnetArgs.builder()
        //     //     .name("PrivateSubnet" + i)
        //     //     .availabilityZone(az)
        //     //     .vpcId(vpc.id())
        //     //     .build());

        //     publicSubnet.setRouteTableId(publicRouteTable.id());
        //     privateSubnet.setRouteTableId(privateRouteTable.id());
        // }

        // Create a public route in the public route table
        new Route("PublicRoute", RouteArgs.builder()
            .routeTableId(publicRouteTable.id())
            .destinationCidrBlock("0.0.0.0/0")
            .gatewayId(internetGateway.id())
            .build());
        
        ParameterGroup pg = new ParameterGroup("csye6225fall23", new ParameterGroupArgs.Builder()
            .family("mysql8.0")
            .description("CSYE6225 Parameter Group")
            .build()
        );//new ParameterGroupArgs.Builder()
            // .family("mysql8.0") 
            // .description("CSYE6225 Parameter Group")
            // .parameters(Map.of( 
            // )) 
            // .build());

        SecurityGroup dbSecurityGroup = new SecurityGroup("DBSecurityGroup", new SecurityGroupArgs.Builder()
            .vpcId(vpc.id())
            .description("DB Security Group")
            .ingress(SecurityGroupIngressArgs.builder()
                .protocol("tcp")
                .fromPort(3306)
                .toPort(3306)
                .cidrBlocks("0.0.0.0/0") // Allow traffic from the internet
                .description("MySQL Port")
                .build())
            .build()
        );
        //List<String> s = new ArrayList<>();

        System.out.println(dbSecurityGroup.id());
       
        
            
        SecurityGroup sg = new SecurityGroup("mySecurityGroup", new SecurityGroupArgs.Builder()
            .vpcId(vpc.id()) // Replace with your VPC ID
            .ingress(SecurityGroupIngressArgs.builder()
                    .protocol("ssh")
                    .fromPort(22)
                    .toPort(22)
                    .cidrBlocks("0.0.0.0/0") // Allow SSH from anywhere
                    .description("SSH")
                    .build())
            .ingress(SecurityGroupIngressArgs.builder()
                    .protocol("http")
                    .fromPort(80)
                    .toPort(80)
                    .cidrBlocks("0.0.0.0/0") // Allow HTTP from anywhere
                    .description("HTTP")
                    .build())
            .ingress(SecurityGroupIngressArgs.builder()
                    .protocol("https")
                    .fromPort(443)
                    .toPort(443)
                    .cidrBlocks("0.0.0.0/0") // Allow HTTPS from anywhere
                    .description("HTTPS")
                    .build())
            .ingress(SecurityGroupIngressArgs.builder()
                    .protocol("tcp")
                    .fromPort(8081)
                    .toPort(8081)
                    .cidrBlocks("0.0.0.0/0") // Allow HTTPS from anywhere
                    .description("Custom")
                    .build())
            .egress(SecurityGroupEgressArgs.builder()
                    .protocol("tcp")
                    .fromPort(3306)
                    .toPort(3306)
                    .cidrBlocks("0.0.0.0/0") // Allow HTTPS from anywhere
                    .description("mysql")
                    .build())
            .build());
        
        String rdsConfig = "{"
            + "\"allocatedStorage\": 20,"
            + "\"storageType\": \"gp2\","
            + "\"engine\": \"mysql\","
            + "\"engineVersion\": \"8.0\","
            + "\"instanceClass\": \"db.t2.micro\","
            + "\"multiAz\": false,"
            + "\"name\": \"csye6225\","
            + "\"username\": \"csye6225\","
            + "\"password\": \"Moscow1327\","
            + "\"publiclyAccessible\": false,"
            + "\"dbSubnetGroupName\": \"YourDBSubnetGroup\","
            + "\"dbName\": \"csye6225\""
            + "}";
        
        Output<String> sgn = subnetGroup.name();

        Output<List<String>> ec2SecGrpOutput = Output.all(sg.id()).applyValue(ids -> ids);

        System.out.println(ec2SecGrpOutput);
                
        Output<List<String>> rdsSecGrpOutput = Output.all(dbSecurityGroup.id()).applyValue(ids -> ids);

        

        String databaseUser = "csye6225";
        String databaseName = "csye6225";
        String databasePassword = "moscow1327";
        
        
        var dbInstance = new com.pulumi.aws.rds.Instance("dbinstance", com.pulumi.aws.rds.InstanceArgs.builder()
            .engine("mysql")
            .allocatedStorage(20)
            .storageType("gp2")
            .engineVersion("8.0")
            .instanceClass("db.t3.micro")
            .skipFinalSnapshot(true)
            .username(databaseUser)
            .password(databasePassword)
            .dbSubnetGroupName(sgn)
            .dbName(databaseName)
            .parameterGroupName(pg.name())
            .vpcSecurityGroupIds(rdsSecGrpOutput)
            .build()
        );
        Output<List<String>> endpoint = Output.all(dbInstance.endpoint()).applyValue(ids -> ids);
        //String endpoint = dbInstance.endpoint().applyValue(null)
        Output<?> dbpointOutput = dbInstance.endpoint().applyValue(ids->ids.split(":")[0]);
        Output<List<String>> db1 = Output.all(dbSecurityGroup.id()).applyValue(ids -> ids);
        


        //System.out.println(dbSecurityGroup.id());
        
            

        // Cluster dbCluster = new Cluster("myRdsCluster", new ClusterArgs.Builder()
        //     .allocatedStorage(20)
        //     .storageType("gp2")
        //     .engine("mysql")
        //     .engineVersion("8.0")
        //     .dbClusterInstanceClass("db.t3.micro")
        //     .skipFinalSnapshot(true)
        //     .masterUsername("csye6225")
        //     .masterPassword("Moscow1327")
        //     .dbSubnetGroupName(dbSubnetGroup.get(0).toString())
        //     .databaseName("csye6225")
        //     .vpcSecurityGroupIds(Arrays.asList(dbSecurityGroup.id().toString()))
        //     .build());
        
        
        //Output<List<String>> rdsSecGrpOutput = dbInstance.endpoint();

        // String script = "#!/bin/bash\n" +
        //     "echo 'DATABASE_NAME="+databaseName+"' >> /etc/environment\n" +
        //     "echo 'DATABASE_USER="+databaseUser+"' >> /etc/environment\n" +
        //     "echo 'DATABASE_PASSWORD="+databasePassword+"' >> /etc/environment\n" +
        //     "echo 'DATABASE_IP="+dbpointOutput+"' >> /etc/environment\n" +
        //     "sudo systemctl daemon-reload"+
        //     "sudo systemctl start webappstart.service" +
        //     "sudo systemctl enable webappstart.service" +
        //     "sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \\\n" + //
        //             "    -a fetch-config \\\n" + //
        //             "    -m ec2 \\\n" + //
        //             "    -c file:/opt/cloudwatch-config.json \\\n" + //
        //             "    -s";
        
        Output<String> script1 = dbpointOutput.applyValue(dbpoint -> {
            return "#!/bin/bash\n" +
                "echo 'DATABASE_NAME="+databaseName+"' >> /etc/environment\n" +
                "echo 'DATABASE_USER="+databaseUser+"' >> /etc/environment\n" +
                "echo 'DATABASE_PASSWORD="+databasePassword+"' >> /etc/environment\n" +
                "echo 'DATABASE_IP="+dbpoint+"' >> /etc/environment\n" +
                "touch custom.properties\n"+
                "sudo touch /opt/csye6225.log\n"+
                "sudo chmod 777 /opt/csye6225.log\n"+
                "echo 'spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver' >> /home/admin/custom.properties\n"+
                "echo 'spring.datasource.url=jdbc:mysql://"+dbpoint+":3306/"+databaseName +"' >> /home/admin/custom.properties\n" +
                "echo 'spring.datasource.username="+databaseUser+"' >> /home/admin/custom.properties\n" +
                "echo 'spring.datasource.password="+databasePassword+"' >> /home/admin/custom.properties\n" +
                "echo 'spring.jpa.show-sql = true' >> /home/admin/custom.properties\n"+
                "echo 'spring.jpa.hibernate.ddl-auto = create' >> /home/admin/custom.properties\n"+
                "echo 'spring.jpa.properties.hibernate.dialect = org.hibernate.dialect.MySQL8Dialect' >> /home/admin/custom.properties\n"+
                "echo 'server.port=8081' >> /home/admin/custom.properties\n"+
                "echo 'logging.file.path=/opt' >> /home/admin/custom.properties\n"+
                "echo 'logging.file.name=/opt/csye6225.log' >> /home/admin/custom.properties\n"+
                "echo 'publish.metrics=true' >> /home/admin/custom.properties\n"+
                "echo 'metrics.statsd.host=localhost' >> /home/admin/custom.properties\n"+
                "echo 'metrics.statsd.port=8125' >> /home/admin/custom.properties\n"+
                "sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \\\n" + 
                "    -a fetch-config \\\n" + 
                "    -m ec2 \\\n" + 
                "    -c file:/opt/cloudwatch-config.json \\\n" + 
                "    -s\n" +
                "sudo systemctl start amazon-cloudwatch-agent\n" +
                "sudo systemctl enable amazon-cloudwatch-agent";
                    });

        
        Role instanceRole = new Role("instanceRole",
            RoleArgs.builder().assumeRolePolicy(
                "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Action\":\"sts:AssumeRole\",\"Principal\":{\"Service\":\"ec2.amazonaws.com\"},\"Effect\":\"Allow\"}]}")
            .build());

        InstanceProfile instanceProfile = new InstanceProfile("instanceProfile", 
        InstanceProfileArgs.builder().role(instanceRole.name()).build());
        
        RolePolicyAttachment cloudWatchPolicy = new RolePolicyAttachment("CloudWatchPolicy",
            RolePolicyAttachmentArgs.builder().role(instanceRole.name()).policyArn("arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy").build());

        
        //System.out.println(script);
        
        String ami = "ami-0d9dd57228a3a3ed7";
        String ami1 = "ami-0c716860a9b4382dc";
        String ami2 = "ami-0e1e60cc5db66582b";
        String ami3 = "ami-0e6758271534692cd";

        String key_pair = "csye6225";

            
        var ec2Instance = new Instance("MyEC2Instance", InstanceArgs.builder()
            .instanceType(InstanceType.T2_Micro)
            .keyName(key_pair)
            //.ami("ami-0bc0d752e4eaeb3fe")  // Replace with your AMI ID
            .ami(ami3)
            .subnetId(publicSub.get(0).id())  
            .vpcSecurityGroupIds(ec2SecGrpOutput)
            .iamInstanceProfile(instanceProfile.id())
            .userData(script1)
            .tags(Map.of("Name", "csye6225-assignment5-Instance1"))
            .build());

        Zone devdnsZone = new Zone("mydevPublicZone", ZoneArgs.builder()
            .name("dev.adityaraj-2703.me")
            .build());

        Zone demodnsZone = new Zone("mydemoPublicZone", ZoneArgs.builder()
            .name("demo.adityaraj-2703.me")
            .build());
        
        Output<List<String>> ec2instanceList = Output.all(ec2Instance.publicIp()).applyValue(publicIp -> publicIp);

        Record aRecorddev = new Record("ARecorddev", RecordArgs.builder()
            .type("A")
            .zoneId(devdnsZone.id())
            .ttl(60)
            .name("")
            .records(ec2instanceList)
            .build());
        
        Record aRecorddemo = new Record("ARecorddemo", RecordArgs.builder()
            .type("A")
            .zoneId(demodnsZone.id())
            .ttl(60)
            .name("")
            .records(ec2instanceList)
            .build());
            
    }
}
