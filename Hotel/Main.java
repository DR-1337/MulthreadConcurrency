package Hotel;
/**
 * Author: Luis Segarra
 * Project #2:  Exploring Multiple Threads
 * Due Date:   Saturday, November 7, 2015
 * Operating Systems CS4348
 * 
 * This purpose of this project is to explore concurrency in a multi-thread environment 
 * Two front desk employees help guests get a room
 * If the guest has more than 2 bags then a bellhop helps them
 * Guests need to wait for a front desk employee to get a room 
 * Guests need to wait for bellhop to get their bags
 * each front desk employee can only help one guest at a time
 * bellhops can only help one guest
 * 
 * Each employee and guest have their own thread
 */
import java.util.concurrent.Semaphore;

public class Main 
{
    private static Semaphore FrontDesk;
    private static Semaphore GetRoom;
    private static Semaphore RoomReady;
    private static Semaphore BellHop;
    private static Semaphore Bags;
    private static Semaphore BagsDelivered;
    private static Semaphore CustReady;
    private static Semaphore CustInfo;
    private static Semaphore KeyReady;
    private int RoomNum,FrontDeskID, CurrentCust, CustIDForBellHop, BellHopId;

    Boolean Running;
	
//GetARoom
//returns the RoomNum available and 
//the frontdesk employee that set it

    int[] GetARoom()
    {	
        int RoomNumberDTO[] = new int[2];
        RoomNumberDTO[0] = RoomNum;
        RoomNumberDTO[1] = FrontDeskID;
        return RoomNumberDTO;		
    }

//FindARoom
// sets the next available Guest Room number and the employee that set it

    int FindARoom(int WhosAsking)
    {
        FrontDeskID = WhosAsking;
        int NextRoom = ++RoomNum;
        return NextRoom;
    }
//NeedBellHop
//Checks if guest needs a bellhop
//if they do they enter critical section

    void NeedBellHop(int CurrentCustomerBags, int CurrentCustomer)
    {
        if(CurrentCustomerBags > 2)
        {	
            try 
            {
//critical section
                Bags.acquire();
                CustIDForBellHop = CurrentCustomer;
                BellHop.release();
                int CurrentBellHop = BellHopId;
                BagsDelivered.acquire();
                System.out.println("Guest " + CurrentCustomer +  " receives bags from bellhop " + CurrentBellHop + " and gives tip ");
            } catch (InterruptedException e) 
            {
                e.printStackTrace();
            }
        }
    }

// Main Constructor	
/*
    Initially there are two frondesk employees ready
    The system is ready for one cust info
    No guests waiting or ready
    No bellhops needed
    No bags delivered
    First Room ready to go
    
    */
    Main(int Guests)
    { 
        FrontDesk = new Semaphore(2, true); 
        CustInfo = new Semaphore(1, true);
        CustReady = new Semaphore(0, true);
        KeyReady = new Semaphore(0,true);
        BellHop = new Semaphore(0, true);
        GetRoom = new Semaphore(0, true);	
        RoomReady = new Semaphore(1, true);
        Bags = new Semaphore(1, true);
        BagsDelivered = new Semaphore(0, true);
        Running = true;
    }

//Guest class	
/*
    Guests know how many bags they have
    and their guest ID (Their Name)
    MyRoomNumber will hold their room number and the name of the front desk employee that helped them
    */
    class Guests implements Runnable
    {
        int Bags;
        int[] MyRoomNumber;
        int GuestId;
        @Override
        public void run()
        {		
            System.out.println("Guest " + GuestId + " enters hotel with " + Bags + " bags");
            try 
            {
//Critical section
                FrontDesk.acquire();
                CustInfo.acquire();
                CurrentCust = GuestId;
                CustReady.release();
                GetRoom.acquire();
                MyRoomNumber = GetARoom();
                RoomReady.release();
                KeyReady.acquire();	
            } catch (InterruptedException e) 
            {
                    e.printStackTrace();
            }	
            System.out.println("Guest " + GuestId + " receives room key for room " + MyRoomNumber[0] + " from front desk employee " + MyRoomNumber[1] );
            NeedBellHop(Bags, GuestId);
            System.out.println("Guest " + GuestId + " enters room " + MyRoomNumber[0]);
            System.out.println("Guest " + GuestId + " Retires for evening");
        }	
//Guest Constructor			
        Guests(int id)
        {
            Bags = (int)(Math.random()*10)%6;
            GuestId = id;
            MyRoomNumber = new int[2];
        }	
    }

//FrontDesk class	

    class FrontDesk implements Runnable
    {
        int EmployeeId;
        int CurrentGuestIDImHelping, GuestsRoom;
        @Override
        public void run() 
        {
            loop:
            while(Running)
            {			
                try {
//critical section
                    CustReady.acquire();
                    if(!Running)
                    {
                        break loop;
                    }
                    CustInfo.release();
                    CurrentGuestIDImHelping = CurrentCust;
                    RoomReady.acquire();
                } catch (InterruptedException e) 
                {
                    e.printStackTrace();
                }		
                GuestsRoom = FindARoom(EmployeeId);
                GetRoom.release();					
                System.out.println("Front Desk Employee " + EmployeeId + " registers guest " + CurrentGuestIDImHelping + " room key " + GuestsRoom);
                KeyReady.release();
                FrontDesk.release();			
            }			
        }
// Constructor	
        FrontDesk(int CurrentEmployee){
            EmployeeId = CurrentEmployee;
        }
    }
//Bellhop Class	

    class BellHop implements Runnable
    {
        int EmployeeId;
        @Override
        public void run()
        {
            loop:
            while(Running)
            {			
                try 
                {
                    BellHop.acquire();
                } catch (InterruptedException e) 
                {
                    e.printStackTrace();
                }
                if(!Running)
                {
                    break loop;
                }
                System.out.println("Bellhop " + EmployeeId + " receives bags from guest " + CustIDForBellHop);
                System.out.println("Bellhop " + EmployeeId + " delivers bags to guest " + CustIDForBellHop);
                Bags.release();
                BagsDelivered.release();
            }
        }
// Constructor		
        BellHop(int id)
        {
            EmployeeId = id;
        }
    }
// Makes sure bellhop and frontdesk finish execution	
    void finish()
    {
        Running = false;
        BellHop.release();
        BellHop.release();
        CustReady.release();
        CustReady.release();
    }

//Main	

    public static void main(String[] args) 
    {
        final int  EXPECTED_GUESTS = 25; 	
        Main MainObject = new Main(EXPECTED_GUESTS);
        Guests GuestObjectArray[] = new Guests[EXPECTED_GUESTS];
        FrontDesk FrontDeskObjectArray[] =  new FrontDesk[2];
        BellHop BellHopObjectArray[] = new BellHop[2];
        Thread GuestThreadArray[] = new Thread [EXPECTED_GUESTS];
        Thread FrontDeskThreadArray[] = new Thread[2];
        Thread BellHopThreadArray[] = new Thread [2];		
        System.out.println("Simulation start");

        for(int EmployeeObjectIndex = 0; EmployeeObjectIndex < 2; EmployeeObjectIndex++)
        {
            BellHopObjectArray[EmployeeObjectIndex] = MainObject.new BellHop(EmployeeObjectIndex+1);
            FrontDeskObjectArray[EmployeeObjectIndex] = MainObject.new FrontDesk(EmployeeObjectIndex+1);
            FrontDeskThreadArray[EmployeeObjectIndex] = new Thread(FrontDeskObjectArray[EmployeeObjectIndex]);
            BellHopThreadArray[EmployeeObjectIndex] = new Thread(BellHopObjectArray[EmployeeObjectIndex]);
            System.out.println("Front desk employee "  +  EmployeeObjectIndex + " created");
            System.out.println("Bellhop "+ EmployeeObjectIndex + " created");
        }
// Create guest objects and threads 
        for(int GuestObjectIndex = 0; GuestObjectIndex < EXPECTED_GUESTS; GuestObjectIndex++)
        {
            GuestObjectArray[GuestObjectIndex] = MainObject.new Guests(GuestObjectIndex);
            GuestThreadArray[GuestObjectIndex] = new Thread(GuestObjectArray[GuestObjectIndex]);
            System.out.println("Guest " + GuestObjectIndex + " created");		
        }
// Thread start loops
        for(int GuestThreadStartIndex = 0; GuestThreadStartIndex < EXPECTED_GUESTS; GuestThreadStartIndex++)
        {
            GuestThreadArray[GuestThreadStartIndex].start();		
        }
        for(int EmployeeThreadIndex = 0; EmployeeThreadIndex < 2; EmployeeThreadIndex++)
        {
            FrontDeskThreadArray[EmployeeThreadIndex].start();
            BellHopThreadArray[EmployeeThreadIndex].start();
        }
//Threads are joined once they are done executing
        for(int GuestThreadJoinIndex = 0; GuestThreadJoinIndex < EXPECTED_GUESTS; GuestThreadJoinIndex++)
        {		
            try 
            {
                GuestThreadArray[GuestThreadJoinIndex].join();
            } catch (InterruptedException e) 
            {
                e.printStackTrace();
            }
            System.out.println("Guest " + GuestThreadJoinIndex + " Joined");	
        }
        MainObject.finish();
        System.out.println("Simulation end");	
    }
}
