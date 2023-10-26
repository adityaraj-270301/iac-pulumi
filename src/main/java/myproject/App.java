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
import com.pulumi.core.Output;

import pulumirpc.Provider.CreateRequest;

import com.pulumi.aws.s3.Bucket;
import com.pulumi.aws.ec2.SubnetArgs;
import com.pulumi.aws.ec2.VpcArgs;
import com.pulumi.aws.ec2.enums.InstanceType;
import com.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs;
import com.pulumi.aws.ec2.outputs.SecurityGroupIngress;
import com.pulumi.aws.Provider;
import com.pulumi.aws.ProviderArgs;

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

            var routeTableAssociationArgsPub = new RouteTableAssociation("RouteTableAssociationPub"+i, RouteTableAssociationArgs.builder().routeTableId(publicRouteTable.id()).subnetId(publicSubnet.id()).build());
            //RouteTableAssociation routeTableAssociationPub = new RouteTableAssociation("publicRouteTableAssociationPub" + i, routeTableAssociationArgsPub);

            Subnet privateSubnet = new Subnet("PrivateSubnet" + i, SubnetArgs.builder()
                .availabilityZone(available.applyValue(getAvailabilityZonesResult -> getAvailabilityZonesResult.names().get(t)))                
                .cidrBlock(privateCidrBlock)
                .vpcId(vpc.id())
                .build());

            var routeTableAssociationArgsPriv = new RouteTableAssociation("RouteTableAssociationPriv"+i, RouteTableAssociationArgs.builder().routeTableId(privateRouteTable.id()).subnetId(privateSubnet.id()).build());
            //RouteTableAssociation routeTableAssociationPriv = new RouteTableAssociation("publicRouteTableAssociationPriv" + i, routeTableAssociationArgsPriv);

        }

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

        var dbSecurityGroup = new SecurityGroup("DBSecurityGroup", SecurityGroupArgs.builder()
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

        String dbSecurityGroupId = dbSecurityGroup.toString();
        System.out.println(dbSecurityGroupId);
        

        SecurityGroup sg = new SecurityGroup("mySecurityGroup", new SecurityGroupArgs.Builder()
            .vpcId(vpc.id()) // Replace with your VPC ID
            .ingress(SecurityGroupIngressArgs.builder()
                    .protocol("tcp")
                    .fromPort(22)
                    .toPort(22)
                    .cidrBlocks("0.0.0.0/0") // Allow SSH from anywhere
                    .description("SSH")
                    .build())
            .ingress(SecurityGroupIngressArgs.builder()
                    .protocol("tcp")
                    .fromPort(80)
                    .toPort(80)
                    .cidrBlocks("0.0.0.0/0") // Allow HTTP from anywhere
                    .description("HTTP")
                    .build())
            .ingress(SecurityGroupIngressArgs.builder()
                    .protocol("tcp")
                    .fromPort(443)
                    .toPort(443)
                    .cidrBlocks("0.0.0.0/0") // Allow HTTPS from anywhere
                    .description("HTTPS")
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
        
        // RdsDbInstance mysqldb = new RdsDbInstance("mysqldbms", new RdsDbInstanceArgs.Builder()
        //     .rdsDbInstanceArn(rdsConfig)
        //     .dbUser("admin")
        //     .stackId("dev")
        //     .dbPassword("Pass1234")
        //     .build()
        // );

        Cluster dbCluster = new Cluster("myRdsCluster", new ClusterArgs.Builder()
            .allocatedStorage(20)
            .storageType("gp2")
            .engine("mysql")
            .engineVersion("8.0")
            .dbClusterInstanceClass("db.t3.micro")
            .skipFinalSnapshot(true)
            .masterUsername("csye6225")
            .masterPassword("Moscow1327")
            .dbSubnetGroupName("")
            .databaseName("csye6225")
            .vpcSecurityGroupIds((Output<List<String>>) List.of(dbSecurityGroup.id()))
            .build());

        

            
        
            
        var ec2Instance = new Instance("MyEC2Instance", InstanceArgs.builder()
            .instanceType(InstanceType.T2_Micro)
            .keyName("pulumi-key-pair")
            .ami("ami-0f2e959ff43a5085a")  // Replace with your AMI ID
            .subnetId("subnet-0a325fe35bf0b984c")  // Associate with a private subnet
            //.vpcSecurityGroupIds((Output<List<String>>) List.of(dbSecurityGroup.id()))  // Attach your security group
            //.vpcSecurityGroupIds("sg-0c74c970282df84ab")
            //.securityGroups(new String[]{dbSecurityGroup.name})
            .vpcSecurityGroupIds((Output<List<String>>) List.of(dbSecurityGroup.id()))
            .userData("#!/bin/bash\nYour user data script here")  // Customize user data script if needed
            .tags(Map.of("Name", "csye6225-assignment5-Instance1"))
            .build());
    }
}
