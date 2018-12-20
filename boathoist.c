#include <stdio.h>
#include <string.h>    //strlen
#include <stdlib.h>    //strlen
#include <sys/socket.h>
#include <arpa/inet.h> //inet_addr
#include <unistd.h>    //write
#include <pthread.h> //for threading , link with lpthread
#include <wiringPi.h>
#include <sys/time.h> //for ultrasonic

#define IN1 0    // wiringPi GPIO0(pin11)  
#define IN2 1    // pin12 
#define IN3 2    // pin13
#define IN4 3    // pin15

#define Trig 4
#define Echo 5

void *check_water(void *);

//the thread function
void *connection_handler(void *);
int create_socket(int port_number);

void raiseHoist();
void lowerHoist();

void update_water_level();
float disMeasure(void);

void setStep(int a, int b, int c, int d);
void stop(void);
void forward(int t, int steps);
void backward(int t, int steps); // 512 steps ---- 360 angle    

float water_level, boat_level;
char boat_status[] = "Boat is on Lift";

int masterBreak = 0;
 
int main(int argc , char *argv[])
{
	
	if (wiringPiSetup() == -1)
		return 1;
	
	/* set pins mode as output */  
	pinMode(IN1, OUTPUT);  
	pinMode(IN2, OUTPUT);  
	pinMode(IN3, OUTPUT);  
	pinMode(IN4, OUTPUT);
	
	pinMode(Echo, INPUT);  
	pinMode(Trig, OUTPUT);
	
    pthread_t water_thread;
	pthread_create(&water_thread, NULL, check_water, NULL);
	
	int success = create_socket(8888);
	
	if(success == 1)
		return 1;
    
   
    return 0;
}

void *check_water(void *vargp)
{
	while(1)
	{
		update_water_level();
		printf("Water Level:%f\n", water_level);
		sleep(1);
	}
	return NULL;
}

int create_socket(int port_number)
{
	int socket_desc , client_sock , c , *new_sock;
    struct sockaddr_in server , client;
     
    //Create socket
    socket_desc = socket(AF_INET , SOCK_STREAM , 0);
    if (socket_desc == -1)
    {
        printf("Could not create socket");
    }
    puts("Socket created");
     
    //Prepare the sockaddr_in structure
    server.sin_family = AF_INET;
    server.sin_addr.s_addr = INADDR_ANY;
    server.sin_port = htons(port_number);
     
    //Bind
    if( bind(socket_desc,(struct sockaddr *)&server , sizeof(server)) < 0)
    {
        //print the error message
        perror("bind failed. Error");
        return 1;
    }
    puts("bind done");
         
    //Listen
    listen(socket_desc , 3);
     
    //Accept and incoming connection
    puts("Waiting for incoming connections...");
    c = sizeof(struct sockaddr_in);
    while( (client_sock = accept(socket_desc, (struct sockaddr *)&client, (socklen_t*)&c)) )
    {
        puts("Connection accepted");
         
        pthread_t sniffer_thread;
        new_sock = malloc(1);
        *new_sock = client_sock;
        
        if( pthread_create( &sniffer_thread , NULL ,  connection_handler , (void*) new_sock) < 0)
        {
            perror("could not create thread");
            return 1;
        }
         
        //Now join the thread , so that we dont terminate before the thread
        //pthread_join( sniffer_thread , NULL);
        puts("Handler assigned");
    }
     
    if (client_sock < 0)
    {
        perror("accept failed");
        return 1;
    }
    
    return 0;
}
 
/*
 * This will handle connection for each client
 * */
void *connection_handler(void *socket_desc)
{
    //Get the socket descriptor
    int sock = *(int*)socket_desc;
    int read_size;
     
	char client_message[2000];
    
    printf("Data received: ");
    //Receive a message from client
    while( (read_size = recv(sock , client_message , 2000 , 0)) > 0 )
    {
		
		puts(client_message);
        
        if(strcmp(client_message, "Raising Hoist") == 0){
			stop();
			masterBreak = 0;
			raiseHoist();
		}else if(strcmp(client_message, "Lowering Hoist") == 0){
			stop();
			masterBreak = 0;
			lowerHoist();
		}else if(strcmp(client_message, "Stopping Hoist") == 0){
			stop();
		}else if(strcmp(client_message, "Get Data") == 0){
			write(sock , boat_status , strlen(boat_status));
			
			puts("Sent Data");
		}else{
			puts("ERROR");
		}
        
    }
     
    if(read_size == 0)
    {
        puts("Client disconnected");
        fflush(stdout);
    }
    else if(read_size == -1)
    {
        perror("recv failed");
    }
         
    //Free the socket pointer
    free(socket_desc);
     
    return 0;
}

void raiseHoist()
{
	puts("Going Up");
	//update_water_level();
	
	int steps = water_level * 1; 
	
	steps = 512;
	
	forward(4, steps);      

	printf("stop...\n");  
	stop();   
}

void lowerHoist()
{
	puts("Going Down");
	//update_water_level();
	
	int steps = water_level * 1;
	
	steps = 512;
 
	backward(2, steps);      

	printf("stop...\n");  
	stop();
}

void update_water_level()
{
	water_level = disMeasure();
}

float disMeasure(void)  
{  
	struct timeval tv1;  
	struct timeval tv2;  
	long start, stop;  
	float dis;  

	digitalWrite(Trig, LOW);  
	delayMicroseconds(2);  

	digitalWrite(Trig, HIGH);  //produce a pluse
	delayMicroseconds(10); 
	digitalWrite(Trig, LOW);  

	while(!(digitalRead(Echo) == 1));  
	gettimeofday(&tv1, NULL);           //current time 

	while(!(digitalRead(Echo) == 0));  
	gettimeofday(&tv2, NULL);           //current time  

	start = tv1.tv_sec * 1000000 + tv1.tv_usec; 
	stop  = tv2.tv_sec * 1000000 + tv2.tv_usec;  

	dis = (float)(stop - start) / 1000000 * 34000 / 2;  //count the distance 

	return dis;  
} 

void setStep(int a, int b, int c, int d)  
{  
	digitalWrite(IN1, a);     
	digitalWrite(IN2, b);     
	digitalWrite(IN3, c);     
	digitalWrite(IN4, d);     
}  

void stop(void)  
{  
	masterBreak = 1;
	setStep(0, 0, 0, 0);      
}  

void backward(int t, int steps)  
{  
	int i;  

	for(i = 0; i < steps && !masterBreak; i++){  
		setStep(1, 0, 0, 0);  
		delay(t);  
		setStep(0, 1, 0, 0);      
		delay(t);  
		setStep(0, 0, 1, 0);      
		delay(t);  
		setStep(0, 0, 0, 1);      
		delay(t);  
	}  
}  

void forward(int t, int steps)  
{  
	int i;  

	for(i = 0; (i < steps) && !masterBreak; i++){  
		setStep(0, 0, 0, 1);  
		delay(t);  
		setStep(0, 0, 1, 0);      
		delay(t);  
		setStep(0, 1, 0, 0);      
		delay(t);  
		setStep(1, 0, 0, 0);      
		delay(t);  
	}  
}  
