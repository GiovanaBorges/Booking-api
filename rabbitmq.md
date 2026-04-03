### Exemplificação Fluxo rabbitmq

````
         +-------------------+
         |  TopicExchange    |
         | bookingExchange   |
         +-------------------+
          |     |     |
          |     |     |
          v     v     v
+---------------+ +---------------+ +---------------+
| bookingCreated| | bookingUpdated | | bookingDeleted|
|    Queue      | |    Queue       | |    Queue      |
+---------------+ +---------------+ +---------------+
          |     |     |
          v     v     v
+---------------+ +---------------+ +---------------+
| bookingCreated| | bookingUpdated | | bookingDeleted|
|    DLQ        | |    DLQ         | |    DLQ        |
+---------------+ +---------------+ +---------------+

````
````
         +-------------------+
         | TopicExchange     |
         | userExchange      |
         +-------------------+
          |     |     |
          v     v     v
+---------------+ +---------------+ +---------------+
| userCreated   | | userUpdated    | | userDeleted   |
| Queue         | | Queue          | | Queue         |
+---------------+ +---------------+ +---------------+
          |     |     |
          v     v     v
+---------------+ +---------------+ +---------------+
| userCreatedDLQ| | userUpdatedDLQ | | userDeletedDLQ|
+---------------+ +---------------+ +---------------+

````
````
         +-------------------+
         | DirectExchange    |
         | providerExchange  |
         +-------------------+
          |     |     |
          v     v     v
+---------------+ +---------------+ +---------------+
| providerCreated| | providerUpdated| | providerDeleted|
| Queue         | | Queue          | | Queue         |
+---------------+ +---------------+ +---------------+
          |                   |     |
          v                   v     v
+---------------+ +---------------+ +---------------+
| providerCreatedDLQ|       | providerUpdatedDLQ| | providerDeletedDLQ|
+---------------+ +---------------+ +---------------+