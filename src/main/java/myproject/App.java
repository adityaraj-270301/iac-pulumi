package myproject;


import com.pulumi.aws.AwsFunctions;
import com.pulumi.aws.Config;
import com.pulumi.Pulumi;
import com.pulumi.Context;
import com.pulumi.aws.ec2.InternetGateway;
import com.pulumi.aws.ec2.InternetGatewayArgs;
import com.pulumi.aws.ec2.Route;
import com.pulumi.aws.ec2.RouteArgs;
import com.pulumi.aws.ec2.RouteTable;
import com.pulumi.aws.ec2.RouteTableArgs;
import com.pulumi.aws.ec2.RouteTableAssociation;
import com.pulumi.aws.ec2.RouteTableAssociationArgs;
import com.pulumi.aws.ec2.Subnet;
import com.pulumi.aws.ec2.SubnetArgs;
import com.pulumi.aws.ec2.Vpc;
import com.pulumi.aws.inputs.GetAvailabilityZonesArgs;
import com.pulumi.core.Output;
import com.pulumi.aws.s3.Bucket;
import com.pulumi.aws.ec2.SubnetArgs;
import com.pulumi.aws.ec2.VpcArgs;
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

        // Pulumi.run(ctx -> {
        //     var bucket = new Bucket("my-bucket");
        //     ctx.export("bucketName", bucket.bucket());
        // });
    }
}
