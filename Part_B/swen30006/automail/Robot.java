package automail;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import strategies.IMailPool;
import strategies.IRobotBehaviour;
import strategies.MyRobotBehaviour;
import java.util.Map;
import java.util.TreeMap;

/**
 * The robot delivers mail!
 */
public abstract class Robot {

	public StorageTube tube;
    public IRobotBehaviour behaviour;
    private final String id;
    /** Possible states the robot can be in */
    public enum RobotState { DELIVERING, WAITING, RETURNING }
    public RobotState current_state;
    private int current_floor;
    private int destination_floor;
    private IMailPool mailPool;
    private boolean isStrong;
    private MailItem deliveryItem;
    private int deliveryCounter;
    

    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     * @param behaviour governs selection of mail items for delivery and behaviour on priority arrivals
     * @param reportDelivery governs the final delivery
     * @param mailPool is the source of mail items
     * @param strong is whether the robot can carry heavy items
     */
    public Robot(boolean strong, int tubeCapacity){

    	id = "R" + hashCode();
    	current_state = RobotState.RETURNING;
        current_floor = Building.MAILROOM_LOCATION;
        this.tube = new StorageTube(tubeCapacity);
        this.behaviour = new MyRobotBehaviour(strong);
        this.isStrong = strong;
        this.deliveryCounter = 0;
    }
    
    public void setMailPool(IMailPool mailPool) {
    	this.mailPool = mailPool;
    }

    /**
     * This is called on every time step
     * @throws ExcessiveDeliveryException if robot delivers more than the capacity of the tube without refilling
     */
    public void step() throws ExcessiveDeliveryException, ItemTooHeavyException{    	
    	switch(current_state) {
    		/** This state is triggered when the robot is returning to the mailroom after a delivery */
    		case RETURNING:
    			/** If its current position is at the mailroom, then the robot should change state */
                if(current_floor == Building.MAILROOM_LOCATION){
                	while(!tube.isEmpty()) {
                		MailItem mailItem = tube.pop();
                		mailPool.addToPool(mailItem);
                        System.out.printf("T: %3d > old addToPool [%s]%n", Clock.Time(), mailItem.toString());
                	}
                	changeState(RobotState.WAITING);
                } else {
                	/** If the robot is not at the mailroom floor yet, then move towards it! */
                    moveTowards(Building.MAILROOM_LOCATION);
                	break;
                }
    		case WAITING:
    			/** Tell the sorter the robot is ready */
    			mailPool.fillStorageTube(tube);
                /** If the StorageTube is ready and the Robot is waiting in the mailroom then start the delivery */
                if(!tube.isEmpty()){
                	// reset delivery counter
                	deliveryCounter = 0; 
        			behaviour.startDelivery();
        			setRoute();
                	changeState(RobotState.DELIVERING);
                }
                break;
    		case DELIVERING:
    			/** Check whether or not the call to return is triggered manually **/
    			boolean wantToReturn = behaviour.returnToMailRoom(tube);
    			if(current_floor == destination_floor){ 
    				// If already here drop off either way
                    /** Delivery complete, report this to the simulator! */
                    Simulation.deliver(deliveryItem);
                    deliveryCounter++;
                    if(deliveryCounter > tube.capacity){
                    	throw new ExcessiveDeliveryException();
                    }
                    /** Check if want to return or if there are more items in the tube*/
                    if(wantToReturn || tube.isEmpty()){
                    	changeState(RobotState.RETURNING);
                    }
                    else{
                        /** If there are more items, set the robot's route to the location to deliver the item */
                        setRoute();
                        changeState(RobotState.DELIVERING);
                    }
    			} 
    			else{
	    			moveTowards(destination_floor);
    			}
                break;
    	}
    }

    /**
     * Sets the route for the robot
     */
    private void setRoute() throws ItemTooHeavyException{
        /** Pop the item from the StorageUnit */
        deliveryItem = tube.pop();
        if (!isStrong && deliveryItem.getWeight() > 2000) throw new ItemTooHeavyException(); 
        /** Set the destination floor */
        destination_floor = deliveryItem.getDestFloor();
    }

    /**
     * Generic function that moves the robot towards the destination
     * @param destination the floor towards which the robot is moving
     */
    private void moveTowards(int destination){
        if(current_floor < destination){
            current_floor++;
        }
        else{
            current_floor--;
        }
    }
    
    /**
     * Prints out the change in state
     * @param nextState the state to which the robot is transitioning
     */
    private void changeState(RobotState nextState){
    	if (current_state != nextState) {
            System.out.printf("T: %3d > %11s changed from %s to %s%n", Clock.Time(), id, current_state, nextState);
    	}
    	current_state = nextState;
    	if(nextState == RobotState.DELIVERING){
            System.out.printf("T: %3d > %11s-> [%s]%n", Clock.Time(), id, deliveryItem.toString());
    	}
    }
    
    //Given fix for standardising robot id
    static int count = 0;
    static Map<Integer, Integer> hashMap = new TreeMap<Integer, Integer>();
    @Override
    public int hashCode() {
      Integer hash0 = super.hashCode();
      Integer hash = hashMap.get(hash0);
      
      if (hash == null) { hash = count++; hashMap.put(hash0, hash); }
      return hash;
    }
}
