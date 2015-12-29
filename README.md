# Protocol Adapter for Device Language Message Specification protocol

### Build Status

[![Build Status](http://54.77.62.182/job/OSGP_Protocol-Adapter-DLMS_master/badge/icon?style=plastic)](http://54.77.62.182/job/OSGP_Protocol-Adapter-DLMS_master)

### Component Description

These components offer an implementation of DLMS. At the moment, it can send and receive jms message from and to the OSGP.

- osgp-device-simulator-dlms, DLMS device simulator
- osgp-dlms, Implementation of DLMS
- osgp-adapter-protocol-dlms, Protocol Adapter

The components have dependencies.

- shared, Common classes used by the Protocol Adapter and Device Simulator
- osgp-dto, Data Transfer Objects
