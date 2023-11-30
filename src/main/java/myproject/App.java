package myproject;


import com.pulumi.aws.AwsFunctions;
import com.pulumi.aws.Config;
import com.pulumi.Pulumi;
import com.pulumi.Context;
import com.pulumi.aws.cloudwatch.MetricAlarm;
import com.pulumi.aws.ec2.CapacityReservationArgs;
import com.pulumi.aws.ec2.Instance;
import com.pulumi.aws.ec2.InstanceArgs;
import com.pulumi.aws.ec2.InternetGateway;
import com.pulumi.aws.ec2.InternetGatewayArgs;
import com.pulumi.aws.ec2.LaunchTemplate;
import com.pulumi.aws.ec2.LaunchTemplateArgs;
import com.pulumi.aws.ec2.NetworkInterfaceArgs;
import com.pulumi.aws.ec2.Route;
import com.pulumi.aws.ec2.RouteArgs;
import com.pulumi.aws.ec2.RouteTable;
import com.pulumi.aws.ec2.RouteTableArgs;
import com.pulumi.aws.ec2.RouteTableAssociation;
import com.pulumi.aws.ec2.RouteTableAssociationArgs;
import com.pulumi.aws.ec2.SecurityGroup;
import com.pulumi.aws.ec2.SecurityGroupArgs;
import com.pulumi.aws.ec2.SecurityGroupRule;
import com.pulumi.aws.ec2.SecurityGroupRuleArgs;
import com.pulumi.aws.ec2.Subnet;
import com.pulumi.aws.ec2.SubnetArgs;
import com.pulumi.aws.ec2.Vpc;
import com.pulumi.aws.inputs.GetAvailabilityZonesArgs;
import com.pulumi.aws.lb.Listener;
import com.pulumi.aws.lb.ListenerArgs;
import com.pulumi.aws.lb.LoadBalancer;
import com.pulumi.aws.lb.LoadBalancerArgs;
import com.pulumi.aws.lb.TargetGroup;
import com.pulumi.aws.opsworks.RdsDbInstance;
import com.pulumi.aws.opsworks.RdsDbInstanceArgs;
import com.pulumi.aws.outputs.GetAvailabilityZonesResult;
import com.pulumi.aws.outputs.GetRegionsResult;
import com.pulumi.aws.rds.Cluster;
import com.pulumi.aws.rds.ClusterArgs;
import com.pulumi.aws.rds.ParameterGroup;
import com.pulumi.aws.rds.ParameterGroupArgs;
import com.pulumi.aws.route53.Record;
import com.pulumi.aws.route53.RecordArgs;
import com.pulumi.aws.route53.Zone;
import com.pulumi.aws.route53.ZoneArgs;
import com.pulumi.aws.route53.inputs.RecordAliasArgs;
import com.pulumi.core.Output;


import java.util.HashMap;
import pulumirpc.Provider.CreateRequest;

import com.pulumi.aws.s3.Bucket;
import com.pulumi.aws.servicediscovery.PublicDnsNamespace;
import com.pulumi.aws.sfn.Alias;
import com.pulumi.aws.sns.Topic;
import com.pulumi.aws.sns.TopicArgs;
import com.pulumi.aws.ec2.SubnetArgs;
import com.pulumi.aws.ec2.VpcArgs;
import com.pulumi.aws.ec2.enums.InstanceType;
import com.pulumi.aws.ec2.enums.ProtocolType;
import com.pulumi.aws.ec2.inputs.LaunchTemplateIamInstanceProfileArgs;
import com.pulumi.aws.ec2.inputs.LaunchTemplateNetworkInterfaceArgs;
import com.pulumi.aws.ec2.inputs.LaunchTemplatePlacementArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupEgressArgs;
import com.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs;
import com.pulumi.aws.gamelift.AliasArgs;
import com.pulumi.aws.iam.InstanceProfile;
import com.pulumi.aws.iam.InstanceProfileArgs;
import com.pulumi.aws.iam.Role;
import com.pulumi.aws.iam.RoleArgs;
import com.pulumi.aws.iam.RolePolicyAttachment;
import com.pulumi.aws.iam.RolePolicyAttachmentArgs;
import com.pulumi.aws.Provider;
import com.pulumi.aws.ProviderArgs;

import com.pulumi.aws.lb.TargetGroupArgs;
import com.pulumi.aws.lb.inputs.ListenerDefaultActionArgs;
import com.pulumi.aws.lb.inputs.TargetGroupHealthCheckArgs;

import com.pulumi.aws.autoscaling.Attachment;
import com.pulumi.aws.autoscaling.AttachmentArgs;
import com.pulumi.aws.autoscaling.Group;
import com.pulumi.aws.autoscaling.GroupArgs;
import com.pulumi.aws.autoscaling.Policy;
import com.pulumi.aws.autoscaling.PolicyArgs;
import com.pulumi.aws.autoscaling.inputs.GroupLaunchTemplateArgs;
import com.pulumi.aws.autoscaling.inputs.GroupMixedInstancesPolicyArgs;
import com.pulumi.aws.autoscaling.inputs.GroupMixedInstancesPolicyLaunchTemplateArgs;
import com.pulumi.aws.autoscaling.inputs.GroupMixedInstancesPolicyLaunchTemplateLaunchTemplateSpecificationArgs;
import com.pulumi.aws.cloudwatch.MetricAlarm;
import com.pulumi.aws.cloudwatch.MetricAlarmArgs;
import com.pulumi.aws.codecatalyst.SourceRepositoryArgs;
import com.pulumi.aws.docdb.SubnetGroup;
import com.pulumi.aws.docdb.SubnetGroupArgs;

import java.net.ProtocolFamily;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.pulumi.gcp.storage.*;
import com.pulumi.gcp.cloudfunctions.inputs.FunctionSecretEnvironmentVariableArgs;
import com.pulumi.gcp.cloudfunctions.inputs.FunctionSourceRepositoryArgs;
import com.pulumi.gcp.cloudfunctions.outputs.FunctionSecretEnvironmentVariable;
import com.pulumi.gcp.iam.*;

import com.pulumi.aws.dynamodb.*;
import com.pulumi.aws.dynamodb.inputs.TableAttributeArgs;
import com.pulumi.aws.lambda.*;
import com.pulumi.gcp.serviceAccount.ServiceAccountFunctions;
import com.pulumi.gcp.serviceAccount.Account;
import com.pulumi.gcp.serviceAccount.Key;


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
        
        Provider awsProvider = new Provider("aws", ProviderArgs.builder().region("us-west-2").build());

        final var available = AwsFunctions.getAvailabilityZones(GetAvailabilityZonesArgs.builder()
            .state("available")
            .build());

        var availabilityZones = AwsFunctions.getAvailabilityZones(GetAvailabilityZonesArgs.builder()
            .state("available")
            .build());
        
        int size = Arrays.asList(availabilityZones).size();
        List<?> a = Arrays.asList(availabilityZones);
        System.out.println(a);
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

        List<Output<String>> subnetIdsPub = new ArrayList<>();
        for(Subnet s : publicSub){
            subnetIdsPub.add(s.id());
        }

        Output<List<String>> subnetIdsOutput = Output.all(subnetIds).applyValue(ids -> ids);

        Output<List<String>> subnetIdsOutputPub = Output.all(subnetIdsPub).applyValue(ids -> ids);

        SubnetGroup subnetGroup = new SubnetGroup("private-subnet-group", SubnetGroupArgs.builder()
            .subnetIds(subnetIdsOutput)
            .description("subnet to access database")
            .build()
        );

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
        );

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
       

        

        SecurityGroup sglb = new SecurityGroup("load balancer", new SecurityGroupArgs.Builder()
            .vpcId(vpc.id()) 
            .ingress(SecurityGroupIngressArgs.builder()
                    .protocol("TCP")
                    .fromPort(80)
                    .toPort(80)
                    .cidrBlocks("0.0.0.0/0") 
                    .description("HTTP")
                    .build())
            .ingress(SecurityGroupIngressArgs.builder()
                    .protocol("TCP")
                    .fromPort(443)
                    .toPort(443)
                    .cidrBlocks("0.0.0.0/0") 
                    .description("HTTPS")
                    .build())
            .egress(SecurityGroupEgressArgs.builder()
                    .protocol("TCP")
                    .fromPort(0)
                    .toPort(0)
                    .protocol("-1")
                    .cidrBlocks("0.0.0.0/0") // Allow HTTPS from anywhere
                    .description("egress rule")
                    .build())
            .build());
        Output<List<String>> lbSecGrpOutput = Output.all(sglb.id()).applyValue(ids -> ids);

        SecurityGroup appSecurityGroup = new SecurityGroup("loadBalancerSecurityGroup", new SecurityGroupArgs.Builder()
                .vpcId(vpc.id())
                .ingress(SecurityGroupIngressArgs.builder()
                        .protocol("tcp")
                        .fromPort(22)
                        .toPort(22)
                        
                        .securityGroups(lbSecGrpOutput)
                        .build())
                .ingress(SecurityGroupIngressArgs.builder()
                    .protocol("tcp")
                    .fromPort(8081)
                    .toPort(8081)
                    
                    .description("Custom")
                    .securityGroups(lbSecGrpOutput)
                    .build())
                .egress(SecurityGroupEgressArgs.builder()
                    .protocol("TCP")
                    .fromPort(0)
                    .toPort(0)
                    .protocol("-1")
                    .cidrBlocks("0.0.0.0/0") // Allow HTTPS from anywhere
                    .description("egress rule")
                    .build())
                .build());

    //     SecurityGroupRule ingressRule8081 = new SecurityGroupRule(
    // "app_SecurityGroup_port_8081",
    // new SecurityGroupRuleArgs
    //     .Builder()
    //     .description("Enable 8081 from internet")
    //     .type("ingress")
    //     .fromPort(8081)
    //     .toPort(8081)
    //     .protocol("TCP")
    //     .sourceSecurityGroupId(sglb.id())
    //     .securityGroupId(appSecurityGroup.id())
    //     .build()
    // );

        // SecurityGroupRule ingressRule22 = new SecurityGroupRule(
        //     "db_load_balancer_port_22",
        //     new SecurityGroupRuleArgs
        //         .Builder()
        //         .description("Enable 22 from internet")
        //         .type("ingress")
        //         .fromPort(22)
        //         .toPort(22)
        //         .protocol("TCP")
        //         .sourceSecurityGroupId(sglb.id())
        //         .securityGroupId(appSecurityGroup.id())
        //         .build()
        // );
                
        Output<List<String>> appSecurityGroupOutput = Output.all(appSecurityGroup.id()).applyValue(ids -> ids);
            
        // SecurityGroup sg = new SecurityGroup("mySecurityGroup", new SecurityGroupArgs.Builder()
        //     .vpcId(vpc.id()) // Replace with your VPC ID
        //     .ingress(SecurityGroupIngressArgs.builder()
        //             .protocol("tcp")
        //             .fromPort(22)
        //             .toPort(22)
        //             .cidrBlocks("0.0.0.0/0") // Allow SSH from anywhere
        //             .description("SSH")
        //             .build())
        //     .ingress(SecurityGroupIngressArgs.builder()
        //             .protocol("tcp")
        //             .fromPort(8081)
        //             .toPort(8081)
        //             .cidrBlocks("0.0.0.0/0") // Allow HTTPS from anywhere
        //             .description("Custom")
        //             .securityGroups(lbSecGrpOutput)
        //             .build())
        //     // .egress(SecurityGroupEgressArgs.builder()
        //     //         .protocol("tcp")
        //     //         .fromPort(3306)
        //     //         .toPort(3306)
        //     //         .cidrBlocks("0.0.0.0/0") // Allow HTTPS from anywhere
        //     //         .description("mysql")
        //     //         .build())
        //     .build());
        
        
        
        Output<String> sgn = subnetGroup.name();

        //Output<List<String>> ec2SecGrpOutput = Output.all(sg.id()).applyValue(ids -> ids);

        //System.out.println(ec2SecGrpOutput);
                
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
        
        
        Output<String> script1 = dbpointOutput.applyValue(dbpoint -> {
            String script = "#!/bin/bash\n" +
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
                return Base64.getEncoder().encodeToString(script.getBytes());
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
        String ami3 = "ami-0e67fa710f3ac2888";

        String key_pair = "csye6225";

        // Provider awsProvider = new Provider("aws", ProviderArgs.builder().region("us-west-2").build());

        // Availability availabilityZones = awsProvider.getAvailabilityZones(AvailabilityZonesArgs.builder().state("available").build());         // Use availability zones as needed        String firstAvailabilityZone = availabilityZones.names().apply(names -> names.get(0));

        
        // Get availability zones in current region
        //GetAvailabilityZonesResult availabilityZones = aws.GetAvailabilityZones.invoke().apply(availabilityZoneResult -> availabilityZoneResult);

     
        

        var launchTemplate = new LaunchTemplate("csye6225LaunchTemplate", LaunchTemplateArgs.builder()
            .imageId(ami3)
            .instanceType("t2.micro")
            .keyName(key_pair)
            .userData(script1)
            .networkInterfaces(LaunchTemplateNetworkInterfaceArgs.builder()
                .subnetId(publicSub.get(0).id())
                .associatePublicIpAddress("true")
                .securityGroups(appSecurityGroupOutput) //check for ID
                .build()
            )
            .placement(LaunchTemplatePlacementArgs.builder()
                .availabilityZone(a.get(0).toString())
                .build()
            )
            .iamInstanceProfile(LaunchTemplateIamInstanceProfileArgs.builder()
                    
                    .arn(instanceProfile.arn())
                    .build()
            )
            //avaiaabilityZone add
            //.vpcSecurityGroupIds(ec2SecGrpOutput)
            
            // .vpcSecurityGroupIds()
            //.availabilityZone(available)
            .build()
        );

        Group csye6225f23_ScalingGroup =  new Group("csye6225fall2023-ScalingGroup", GroupArgs.builder()
            .mixedInstancesPolicy(GroupMixedInstancesPolicyArgs.builder()
                    .launchTemplate(GroupMixedInstancesPolicyLaunchTemplateArgs.builder()
                        .launchTemplateSpecification(GroupMixedInstancesPolicyLaunchTemplateLaunchTemplateSpecificationArgs.builder()
                            .launchTemplateId(launchTemplate.id())
                            .version("$Latest")
                            .build()
                        )
                        .build()
                    )
                    .build()
            )
            
            .minSize(1)
            .maxSize(3)
            .desiredCapacity(1)
            .name("asg_launch_config")
            .vpcZoneIdentifiers(subnetIdsOutputPub)
            .defaultCooldown(60)
            .healthCheckType("EC2")
            .healthCheckGracePeriod(300)
            .build()
        
        );

        Policy scaleup = new Policy("scaleUp", PolicyArgs.builder()
            .autoscalingGroupName(csye6225f23_ScalingGroup.name())
            .scalingAdjustment(1)
            .policyType("SimpleScaling")
            .adjustmentType("ChangeInCapacity")
            .cooldown(300)
            
            .build()
        );
        Policy scaledown = new Policy("scaleDown", PolicyArgs.builder()
            .autoscalingGroupName(csye6225f23_ScalingGroup.name())
            .scalingAdjustment(-1)
            .policyType("SimpleScaling")
            .adjustmentType("ChangeInCapacity")
            .cooldown(300)
            .build()
        );

        Output<List<String>> csye6225f23_ScalingGroupOutput = Output.all(csye6225f23_ScalingGroup.arn()).applyValue(ids -> ids);

        Output<List<String>> scaleupOutput = Output.all(scaleup.arn()).applyValue(ids -> ids);
        Output<List<String>> scaleDownOutput = Output.all(scaledown.arn()).applyValue(ids -> ids);

        MetricAlarm scaleUpAlarm = new MetricAlarm("scaleUpAlarm", MetricAlarmArgs.builder()
            .comparisonOperator("GreaterThanOrEqualToThreshold")
            .evaluationPeriods(2)
            .metricName("CPUUtilization")
            .namespace("AWS/EC2")
            .period(60)
            .statistic("Average")
            .threshold(3.0)
            .actionsEnabled(true)
            .unit("Percent")
            .alarmActions()
            .build() 
        );

        MetricAlarm scaleDownAlarm = new MetricAlarm("scaleDownAlarm", MetricAlarmArgs.builder()
            .comparisonOperator("LessThanThreshold")
            .evaluationPeriods(2)
            .metricName("CPUUtilization")
            .namespace("AWS/EC2")
            .period(60)
            .statistic("Average")
            .threshold(1.0)
            .actionsEnabled(true)
            .unit("Percent")
            //.dimensions(DimensionArgs)
            .alarmActions(scaleDownOutput)
            .build() 
        );

        

        // TargetGroup tg = new TargetGroup("exampleTargetGroup", new TargetGroupArgs.Builder()
        //     .vpcId(vpc.id())
        //     .port(80) // Application listens on port 80
        //     .protocol("HTTP")
        //     .targetHealthStates(new TargetGroupTargetHealthStateArgs.Builder() 
        //         .enableUnhealthyConnectionTermination(false)
        //         .build())
            
        //     .healthCheck(new TargetGroupHealthCheckArgs.Builder()
        //         .path("/healthz")
        //         .matcher("200")
        //         .healthyThreshold(2)
        //         .unhealthyThreshold(3)
        //         .interval(15)
        //         .timeout(5)
                
        //         .build()
        //     )
            
            
        //     .build()
        // );

        

        // TargetGroup tg = new TargetGroup("TargetGroup", TargetGroupArgs.builder()
        //     .vpcId(vpc.id())
        //     .port(8081)
        //     .protocol("HTTP")
        //     .targetType("instance")
        //     .targetHealthStates(Arrays.asList(TargetGroupTargetHealthStateArgs.builder() 
        //             .enableUnhealthyConnectionTermination(true)
        //         .build()))
        //     .healthCheck(TargetGroupHealthCheckArgs.builder()
        //         .path("/healthz")
        //         .protocol("HTTP")
        //         .healthyThreshold(5)
        //         .unhealthyThreshold(5)
                
        //         .build()
        //     )
            
            
        //     .build()
        // );
        //Output<List<TargetGroupTargetHealthState>> targetHealths = tg1.targetHealthStates();

        
        com.pulumi.aws.alb.TargetGroup tg1 = new com.pulumi.aws.alb.TargetGroup("TargetGroup", com.pulumi.aws.alb.TargetGroupArgs.builder()
            .vpcId(vpc.id())
            .port(8081)
            .protocol("HTTP")
            .targetType("instance")
            // .targetHealthStates(Arrays.asList(com.pulumi.aws.alb.inputs.TargetGroupTargetHealthStateArgs.builder() 
            //         .enableUnhealthyConnectionTermination(true)
            //     .build()))
            .healthCheck(com.pulumi.aws.alb.inputs.TargetGroupHealthCheckArgs.builder()
                .path("/")
                .protocol("HTTP")
                .healthyThreshold(5)
                .unhealthyThreshold(5)
                
                .build()
            )
            
            
            .build()
        );


        // LoadBalancer apploadBalancer = new LoadBalancer("webappLoadBalancer", LoadBalancerArgs.builder()
        //     .loadBalancerType("application")
        //     .subnets(subnetIdsOutputPub)
        //     .internal(false)
        //     .securityGroups(lbSecGrpOutput)
            
        //     .build()
        // );


        com.pulumi.aws.alb.LoadBalancer apploadBalancer1 = new com.pulumi.aws.alb.LoadBalancer("webappLoadBalancer", com.pulumi.aws.alb.LoadBalancerArgs.builder()
            .loadBalancerType("application")
            .subnets(subnetIdsOutputPub)
            .internal(false)
            .securityGroups(lbSecGrpOutput)
            
            .build()
        );
        

        

        

        

       



        Output<List<String>> tgOutput = Output.all(tg1.arn()).applyValue(ids -> ids);

        

        

        
        


        // Listener httpListener = new Listener("http-listener", ListenerArgs.builder()
        //     .loadBalancerArn(apploadBalancer.arn())
        //     .port(8081)
        //     .protocol("HTTP")
            
        //     .defaultActions(new ListenerDefaultActionArgs.Builder()
        //         .type("forward")
        //         .targetGroupArn(tg.arn())
        //         .build()
        //     )
        //     .build()
        // );

        com.pulumi.aws.alb.Listener httpl = new com.pulumi.aws.alb.Listener("Listener",com.pulumi.aws.alb.ListenerArgs.builder()
            .loadBalancerArn(apploadBalancer1.arn())
            .port(80)
            .protocol("HTTP")
            
            .defaultActions(com.pulumi.aws.alb.inputs.ListenerDefaultActionArgs.builder()
                .type("forward") 
                .targetGroupArn(tg1.arn())
                .build()
            )
            .build()
        );
        Attachment autoScaingAttachment = new Attachment("example", AttachmentArgs.builder()        
            .autoscalingGroupName(csye6225f23_ScalingGroup.name())
            .lbTargetGroupArn(tg1.arn())
            .build());

            
        // var ec2Instance = new Instance("MyEC2Instance", InstanceArgs.builder()
        //     .instanceType(InstanceType.T2_Micro)
        //     .keyName(key_pair)
        //     //.ami("ami-0bc0d752e4eaeb3fe")  // Replace with your AMI ID
        //     .ami(ami3)
        //     .subnetId(publicSub.get(0).id())  
        //     .vpcSecurityGroupIds(ec2SecGrpOutput)
        //     .iamInstanceProfile(instanceProfile.id())
        //     .userData(script1)
        //     .tags(Map.of("Name", "csye6225-assignment5-Instance1"))
        //     .build());

        Zone devdnsZone = new Zone("mydevPublicZone", ZoneArgs.builder()
            .name("dev.adityaraj-2703.me")
            .build());

        Zone demodnsZone = new Zone("mydemoPublicZone", ZoneArgs.builder()
            .name("demo.adityaraj-2703.me")
            .build());
        
        //Output<List<String>> ec2instanceList = Output.all(ec2Instance.publicIp()).applyValue(publicIp -> publicIp);

        // Record aRecorddev = new Record("ARecorddev", RecordArgs.builder()
        //     .type("A")
        //     .zoneId(devdnsZone.id())
        //     .ttl(60)
        //     .name("")
        //     .records(csye6225fall20203_ScalingGroupOutput)
        //     //edit here only
        //     .aliases(RecordAliasArgs.builder()
        //         .name(apploadBalancer.dnsName())
        //         .zoneId(apploadBalancer.zoneId())
        //         .evaluateTargetHealth(true)
        //         .build()
        //     )
        // .build());
        
        Record aRecorddemo = new Record("ARecorddemo", RecordArgs.builder()
            .type("A")
            .zoneId(demodnsZone.id())
            //.ttl(60)
            .name("demo.adityaraj-2703.me")
            .allowOverwrite(true)
            //.records(csye6225f23_ScalingGroupOutput)
            .aliases(RecordAliasArgs.builder()
                .name(apploadBalancer1.dnsName())
                .zoneId(apploadBalancer1.zoneId())
                .evaluateTargetHealth(true)
                .build()
            )
        .build());

        Topic mySnsTopic = new Topic("SnsTopic-csye6225",TopicArgs.builder()
            .build()
        );

        String existingBucketName = "csye6225gcp-bucket"; 
        Output<String> existingBucketNameOutput = Output.of(existingBucketName);
        com.pulumi.gcp.storage.Bucket existingBucket = com.pulumi.gcp.storage.Bucket.get("existing_bucket", existingBucketNameOutput,null,null);


        com.pulumi.gcp.serviceAccount.Account gcpServiceAccount = new  com.pulumi.gcp.serviceAccount.Account("myServiceAccount",com.pulumi.gcp.serviceAccount.AccountArgs.builder()
            .project("academic-osprey-406507")
            .accountId("academic-osprey-406507")
        .build());

        
        com.pulumi.gcp.serviceAccount.Key gcpServiceAccountKey = new  com.pulumi.gcp.serviceAccount.Key("myServiceAccount",com.pulumi.gcp.serviceAccount.KeyArgs.builder()
            
            .serviceAccountId(gcpServiceAccount.name())
        .build());

        List<String> permissions = new ArrayList<>();
        permissions.add("cloudfunctions.functions.invoke");

        Output<List<String>> permissionsOutput = Output.of(permissions).applyValue(ids->ids);

        com.pulumi.gcp.projects.IAMCustomRole gcpRole = new com.pulumi.gcp.projects.IAMCustomRole("gcpRole",com.pulumi.gcp.projects.IAMCustomRoleArgs.builder()
            .roleId("myapplambdarole")
            .project("academic-osprey-406507")
            .title("LambdaRole")
            .description("IAM role for lambda Function")
            .permissions(permissionsOutput)
        .build()
        );

        com.pulumi.gcp.projects.IAMPolicy gcpPolicy = new com.pulumi.gcp.projects.IAMPolicy("gcpPolicy",com.pulumi.gcp.projects.IAMPolicyArgs.builder()
            .project("academic-osprey-406507")
            .policyData(Output.format("{\"bindings\":[{\"role\":\"%s\",\"members\":[\"user:aditya.3342@gmail.com\"]}]}", gcpRole.name()))
        .build()
        );

        Map<String,Object> environmentSecrets = new HashMap<>();
        environmentSecrets.put("EMAIL_SERVER_SECRET","emailServerSecret");

        Output<Map<String, Object>> environmentSecretsOutput = Output.of(environmentSecrets);

        
        Output<String> deployedURL = Output.of("https://github.com/adityaraj-270301/serverless.git");
        
        

        
        com.pulumi.gcp.cloudfunctions.Function lambdaFunction = new com.pulumi.gcp.cloudfunctions.Function("lambdaFunction", com.pulumi.gcp.cloudfunctions.FunctionArgs.builder()
            .project("academic-osprey-406507")
            .region("us-east-1")
            .runtime("nodejs14")
            // .sourceRepository(FunctionSourceRepositoryArgs.builder()
            //     .deployedUrl(deployedURL)
            
            // .build()
            // )
            .sourceArchiveBucket(existingBucketName)
            .sourceArchiveObject("/Users/adityaraj/Documents/CSYE6225 Network Structures & Cloud Computing/serverless-l.zip")
            .entryPoint("index.js")
            
            .environmentVariables(environmentSecretsOutput)
        .build()
        );

        Table dynamoDbTable = new Table("myDynamoDBTable",TableArgs.builder()
            .name("DynamoTable")
            .attributes(TableAttributeArgs.builder()
                .name("ID")
                .type("S")
            .build()
            )
            .hashKey("ID")
            .billingMode("PAY_PER_REQUEST")
        .build()
        );
            
    }
}
