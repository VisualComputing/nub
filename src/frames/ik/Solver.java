/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Sebastian Chaparro, https://github.com/sechaparroc
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package frames.ik;

import frames.timing.TimingTask;

/**
 * A Solver is a convenient class to solve IK problem
 * Given a Chain or a Tree Structure of Frames, this class will
 * solve the configuration that the frames must have to reach
 * a desired position
 */

public  abstract class Solver {
    protected float ERROR = 0.01f;
    protected int MAXITER = 200;
    protected float MINCHANGE = 0.01f;
    protected float TIMESPERFRAME = 1.f;
    protected float FRAMECOUNTER = 0;
    protected int iterations = 0;
    protected TimingTask executionTask;

    public void restartIterations(){
        iterations = 0;
    }

    public float getERROR() {
        return ERROR;
    }

    public void setERROR(float ERROR) {
        this.ERROR = ERROR;
    }

    public int getMAXITER() {
        return MAXITER;
    }

    public void setMAXITER(int MAXITER) {
        this.MAXITER = MAXITER;
    }

    public float getMINCHANGE() {
        return MINCHANGE;
    }

    public void setMINCHANGE(float MINCHANGE) {
        this.MINCHANGE = MINCHANGE;
    }

    public float getTIMESPERFRAME() {
        return TIMESPERFRAME;
    }

    public void setTIMESPERFRAME(float TIMESPERFRAME) {
        this.TIMESPERFRAME = TIMESPERFRAME;
    }

    public float getFRAMECOUNTER() {
        return FRAMECOUNTER;
    }

    public void setFRAMECOUNTER(float FRAMECOUNTER) {
        this.FRAMECOUNTER = FRAMECOUNTER;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public TimingTask getExecutionTask() {
        return executionTask;
    }

    public void setExecutionTask(TimingTask executionTask) {
        this.executionTask = executionTask;
    }

    public Solver(){
        executionTask = new TimingTask() {
            @Override
            public void execute() {
                solve();
            }
        };
    }


    /*Performs an Iteration of Solver Algorithm */
    public abstract boolean iterate();
    public abstract void update();
    public abstract boolean stateChanged();
    public abstract void reset();

    public boolean solve(){
        //Reset counter
        if(stateChanged()){
            reset();
        }

        if(iterations == MAXITER){
            return true;
        }
        FRAMECOUNTER += TIMESPERFRAME;

        while(Math.floor(FRAMECOUNTER) > 0){
            //Returns a boolean that indicates if a termination condition has been accomplished
            if(iterate()){
                iterations = MAXITER;
                break;
            }
            else iterations+=1;
            FRAMECOUNTER -= 1;
        }
        //update positions
        update();
        return false;
    }
}