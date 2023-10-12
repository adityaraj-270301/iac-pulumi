package assignment4.src.main.java.myproject;

import java.io.ObjectInputFilter.Config;

import java.io.ObjectInputFilter.Config;

import java.io.ObjectInputFilter.Config;

import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.aws.s3.Bucket;


import com.pulumi.Stack;
import com.pulumi.Config;
import com.pulumi.AWS.Vpc;
import com.pulumi.AWS.Subnet;
import com.pulumi.AWS.InternetGateway;
import com.pulumi.AWS.RouteTable;
import com.pulumi.AWS.Route;
import com.pulumi.AWS.Provider;
import com.pulumi.AWS.ProviderArgs;
import com.pulumi.AWS.ProviderArgs.builder;

public class App {
    public static void main(String[] args) {

        Config config = new Config();

        // Define parameters such as AWS region and CIDR blocks
        String awsRegion = config.require("aws:region");
        String vpcCidrBlock = config.require("vpcCidrBlock");

        // Create the VPC
        Vpc vpc = new Vpc("MyVPC", Vpc.Args.builder()
            .cidrBlock(vpcCidrBlock)
            .enableDnsSupport(true)
            .enableDnsHostnames(true)
            .build());

        // Create public and private subnets
        for (int i = 1; i <= 3; i++) {
            String az = String.format("%s%s", awsRegion, (char) (i + 96));
            String publicCidrBlock = String.format("10.0.%d.0/24", i);
            String privateCidrBlock = String.format("10.0.%d.128/24", i);

            Subnet publicSubnet = new Subnet("PublicSubnet" + i, Subnet.Args.builder()
                .availabilityZone(az)
                .cidrBlock(publicCidrBlock)
                .vpcId(vpc.id)
                .mapPublicIpOnLaunch(true)
                .build());

            Subnet privateSubnet = new Subnet("PrivateSubnet" + i, Subnet.Args.builder()
                .availabilityZone(az)
                .cidrBlock(privateCidrBlock)
                .vpcId(vpc.id)
                .build());
        }

        // Create an Internet Gateway and attach it to the VPC
        InternetGateway internetGateway = new InternetGateway("MyInternetGateway", InternetGateway.Args.builder()
            .vpcId(vpc.id)
            .build());

        // Create public and private route tables
        RouteTable publicRouteTable = new RouteTable("PublicRouteTable", RouteTable.Args.builder()
            .vpcId(vpc.id)
            .build());

        RouteTable privateRouteTable = new RouteTable("PrivateRouteTable", RouteTable.Args.builder()
            .vpcId(vpc.id)
            .build());

        // Attach public and private subnets to their respective route tables
        for (int i = 1; i <= 3; i++) {
            Subnet publicSubnet = Subnet.get("PublicSubnet" + i, i);
            Subnet privateSubnet = Subnet.get("PrivateSubnet" + i, i);

            publicSubnet.setRouteTableId(publicRouteTable.id);
            privateSubnet.setRouteTableId(privateRouteTable.id);
        }

        // Create a public route in the public route table
        new Route("PublicRoute", Route.Args.builder()
            .routeTableId(publicRouteTable.id)
            .destinationCidrBlock("0.0.0.0/0")
            .gatewayId(internetGateway.id)
            .build());

        Pulumi.run(ctx -> {
            var bucket = new Bucket("my-bucket");
            ctx.export("bucketName", bucket.bucket());
        });
    }
}
