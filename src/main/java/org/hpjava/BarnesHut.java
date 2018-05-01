
package org.hpjava;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;

import java.util.Random ;

import java.awt.* ;
import javax.swing.* ;

// Single precision version of sequential Java Barnes Hut code.

public class BarnesHut {

    // Size of simulation

    //final static int N = 2000 ;  // Number of "stars"
    final static int N = 250000 ;  // Number of "stars"

    final static float BOX_WIDTH = 100.0F ;

    final static int P = 8 ;

    // Initial state

    final static float RADIUS = 20.0F ;  // of randomly populated sphere

    //final static float ANGULAR_VELOCITY = 0.4F ;
    //final static float ANGULAR_VELOCITY = 3F ;
    final static float ANGULAR_VELOCITY = 1.5F ;
           // controls total angular momentum (tend to increase this
           // as N increases, to keep "galaxy" stable).


    // Simulation

    final static float DT = 0.0005F ;  // Time step
           // (tend to decrease this as N increases, to maintain accuracy)


    // Display

    final static int WINDOW_SIZE = 1000 ;
    final static int DELAY = 0 ;
    final static int OUTPUT_FREQ = 1 ;


    // Star positions
    static float [] x = new float [N] ;
    static float [] y = new float [N] ;
    static float [] z = new float [N] ;

    // Star velocities
    static float [] vx = new float [N] ;
    static float [] vy = new float [N] ;
    static float [] vz = new float [N] ;

    // Star accelerations
    static float [] ax = new float [N] ;
    static float [] ay = new float [N] ;
    static float [] az = new float [N] ;

    // Barnes Hut tree
    static Node tree ;
    
    static Display display = new Display() ;
    
    public static void main(String args []) throws Exception {



        // Define initial state of stars

        Random rand = new Random(1234) ;

        // Randomly choose plane for net angular velocity

        float nx = 2 * rand.nextFloat() - 1 ;
        float ny = 2 * rand.nextFloat() - 1 ;
        float nz = 2 * rand.nextFloat() - 1 ;
        float norm = 1.0F / (float) Math.sqrt(nx * nx + ny * ny + nz * nz) ;
        nx *= norm ;
        ny *= norm ;
        nz *= norm ;


        // ... or just rotate in x, y plane
        //float nx = 0, ny = 0, nz = 1.0 ;

        // ... or just rotate in x, z plane
        //float nx = 0, ny = 1.0, nz = 0 ;

        for(int i = 0 ; i < N ; i++) {

            // Place star randomly in sphere of specified radius
            float rx, ry, rz, r ;
            do {
                rx = (2 * rand.nextFloat() - 1) * RADIUS ;
                ry = (2 * rand.nextFloat() - 1) * RADIUS ;
                rz = (2 * rand.nextFloat() - 1) * RADIUS ;
                r = (float) Math.sqrt(rx * rx + ry * ry + rz * rz) ;
            } while(r > RADIUS) ;

            x [i] = 0.5F * BOX_WIDTH + rx ;
            y [i] = 0.5F * BOX_WIDTH + ry ;
            z [i] = 0.5F * BOX_WIDTH + rz ;

            vx [i] = ANGULAR_VELOCITY * (ny * rz - nz * ry) ; 
            vy [i] = ANGULAR_VELOCITY * (nz * rx - nx * rz) ; 
            vz [i] = ANGULAR_VELOCITY * (nx * ry - ny * rx) ; 
        }
         
        int iter = 0 ;
        while(true) {
            float dtOver2 = 0.5F * DT;
            float dtSquaredOver2 = 0.5F * DT * DT;  

            if(iter % OUTPUT_FREQ == 0) {
                System.out.println("iter = " + iter + ", time = " + iter * DT) ;
                display.repaint() ;
            }

            // Verlet integration:
            // http://en.wikipedia.org/wiki/Verlet_integration#Velocity_Verlet


            for (int i = 0; i < N; i++) {
                // update position
                // mod implements periodic box
                x[i] = mod(x [i] + (vx[i] * DT) + (ax[i] * dtSquaredOver2),
                           BOX_WIDTH);
                y[i] = mod(y [i] + (vy[i] * DT) + (ay[i] * dtSquaredOver2),
                           BOX_WIDTH);
                z[i] = mod(z [i] + (vz[i] * DT) + (az[i] * dtSquaredOver2),
                           BOX_WIDTH);
                // update velocity halfway
                vx[i] += (ax[i] * dtOver2);
                vy[i] += (ay[i] * dtOver2);
                vz[i] += (az[i] * dtOver2);
            }    

            computeAccelerations();

            for (int i = 0; i < N; i++) {
                // finish updating velocity with new acceleration
                vx[i] += (ax[i] * dtOver2);
                vy[i] += (ay[i] * dtOver2);
                vz[i] += (az[i] * dtOver2);
            }

            iter++ ;
        }       


    }

    // Compute accelerations of all stars from current positions:
    static void computeAccelerations() {
        
        long startTreeTime = System.currentTimeMillis();
        
       
        tree = new Node(BOX_WIDTH / 2, BOX_WIDTH / 2, BOX_WIDTH / 2,
                        BOX_WIDTH) ;
        for (int i = 0; i < N; i++) {
            tree.addParticle(x [i], y [i], z [i]);
        }
        tree.preCompute() ;
        System.out.println("Number of nodes = " + tree.treeSize());
       
        long endTreeTime = System.currentTimeMillis();
        
        System.out.println("time to build Tree = " +
                           (endTreeTime - startTreeTime) + " milliseconds"); 
        
        // Interaction forces (gravity)
        // This is where the program spends most of its time.
        long startForceTime = System.currentTimeMillis();
        


        for (int i = 0; i < N; i++) {     
            Vector acc = new Vector() ;
            tree.calcForce(acc, x [i], y [i], z [i]) ;
            ax [i] = acc.x ;
            ay [i] = acc.y ;
            az [i] = acc.z ;
            //break ;  // debug
        }
        
        long endForceTime = System.currentTimeMillis();
        
        System.out.println("time to calculate forces = " +
                           (endForceTime - startForceTime) + " milliseconds");
    }
    
    static class Display extends JPanel {

        static final float SCALE = WINDOW_SIZE / BOX_WIDTH ;

        Display() {

            setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE)) ;

            JFrame frame = new JFrame("MD");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(this);
            frame.pack();
            frame.setVisible(true);
        }

        public void paintComponent(Graphics g) {
            g.setColor(Color.BLACK) ;
            g.fillRect(0, 0, WINDOW_SIZE, WINDOW_SIZE) ;
            g.setColor(Color.WHITE) ;
            for(int i = 0 ; i < N ; i++) {
                int gx = (int) (SCALE * x [i]) ;
                int gy = (int) (SCALE * y [i]) ;
                if(0 <= gx && gx < WINDOW_SIZE && 0 < gy && gy < WINDOW_SIZE) { 
                    g.fillRect(gx, gy, 1, 1) ;
                }
            } 
        }
    }
    
    static float mod(float x, float box) {
        float reduced = x - ((int) (x / box) * box) ;
        return reduced >= 0 ? reduced : reduced + box ;
    }


    static class Node {

        // Barnes-Hut tree node

        final static float OPENING_ANGLE = 1.0F ;

        //float xLo, xHi, yLo, yHi, zLo, zHi ;
        float xMid, yMid, zMid ;
        float size ;
        //float ax, ay, az ;
        int nParticles ;
        float xCent, yCent, zCent ;  // centre of mass
        Node [] children ;

        float threshold ;

        //static int nCellsOpened ; // debug

        Node(float xMid, float yMid, float zMid, float size) {
            this.xMid = xMid ;
            this.yMid = yMid ;
            this.zMid = zMid ;
            this.size = size ;
        }

        void addParticle(float x, float y, float z) {
            /* In single precision following test sometimes fails through rounding errors
            float sizeBy2 = size / 2 ;
            if(x < xMid - sizeBy2 || x > xMid + sizeBy2 ||
               y < yMid - sizeBy2 || y > yMid + sizeBy2 ||
               z < zMid - sizeBy2 || z > zMid + sizeBy2) {
                System.out.println("x = " + x + ", y = " + y + ", z = " + z) ;  // debug
                throw new IllegalArgumentException("particle position outside " +
                                                   "bounding box of Node") ;
            }
            */
            if(nParticles == 0) {
                xCent = x ;
                yCent = y ;
                zCent = z ;
                nParticles = 1 ;
                return ;
            } 
            if(nParticles == 1) {
                children = new Node [8] ;
                addParticleToChild(xCent, yCent, zCent) ;  
            }
            addParticleToChild(x, y, z) ;  
            nParticles++ ;
        }

        void addParticleToChild(float x, float y, float z) {

            int childIdx = ((x < xMid) ? 0 : 4) + ((y < yMid) ? 0 : 2) +
                           ((z < zMid) ? 0 : 1) ;

            Node child = children [childIdx] ;
            if(child == null) {
                float sizeBy4 = size / 4 ;
                child = new Node((x < xMid) ? xMid - sizeBy4 : xMid + sizeBy4,
                                 (y < yMid) ? yMid - sizeBy4 : yMid + sizeBy4,
                                 (z < zMid) ? zMid - sizeBy4 : zMid + sizeBy4,
                                 size / 2) ;
                children [childIdx] = child ;
            }
            child.addParticle(x, y, z) ;
        }

        void preCompute() {

            // Precompute Centre of Mass of this node (where non-leaf node)
            // and opening threshold for force calculation.

            if(children != null) {  
                float xSum = 0, ySum = 0, zSum = 0 ;

                for(int i = 0 ; i < 8 ; i++) {
                    Node child = children [i] ;
                    if(child != null) {
                        child.preCompute() ;
                        int nChild = child.nParticles ;
                        xSum += nChild * child.xCent ;
                        ySum += nChild * child.yCent ;
                        zSum += nChild * child.zCent ;
                    }
                }
                xCent = xSum / nParticles ;
                yCent = ySum / nParticles ;
                zCent = zSum / nParticles ;
            }

            float delta = distance(xCent, yCent, zCent) ;
            threshold = size / OPENING_ANGLE + delta ;
        }

        void calcForce(Vector a, float x, float y, float z) {
            
            if(nParticles == 0)
                throw new RuntimeException("Node without any particles") ;
            if(nParticles == 1) {
                if(x == xCent && y == yCent && z == zCent) {
                    return ;
                }
                else {
                    forceLaw(a, x, y, z) ;
                    //System.out.println("a.x = " + a.x) ;  // debug
                    return ;
                }
            }

            float r = distance(x, y, z) ;
            if(r > threshold) {
                forceLaw(a, x, y, z) ;
                //System.out.println("a.x = " + a.x) ;  // debug
            }
            else {
                for(int n = 0 ; n < 8 ; n++) {
                    Node child = children [n] ;
                    if(child != null) {
                        child.calcForce(a, x, y, z) ;
                    }
                }
            }
        }

        float distance(float x, float y, float z) {

            // Distance from mid-point of this node (use min distance in
            // periodic box).

            float dx, dy, dz;  // separations in x and y directions
            float dx2, dy2, dz2, rSquared ;
            dx = x - xMid ;
            if(dx > BOX_WIDTH / 2) dx -= BOX_WIDTH ;
            if(dx < -BOX_WIDTH / 2) dx += BOX_WIDTH ;
            dy = y - yMid ;
            if(dy > BOX_WIDTH / 2) dy -= BOX_WIDTH ;
            if(dy < -BOX_WIDTH / 2) dy += BOX_WIDTH ;
            dz = z - zMid ;
            if(dz > BOX_WIDTH / 2) dz -= BOX_WIDTH ;
            if(dz < -BOX_WIDTH / 2) dz += BOX_WIDTH ;
            dx2 = dx * dx;
            dy2 = dy * dy;
            dz2 = dz * dz;
            rSquared = dx2 + dy2 + dz2 ;
            return (float) Math.sqrt(rSquared) ;
        }

        void forceLaw(Vector a, float x, float y, float z) {

            // Force exerted by effective mass at CM of this node.

            float dx, dy, dz;  // separations in x and y directions
            float dx2, dy2, dz2, rSquared, r, massRCubedInv;      

            // Vector version of inverse square law
            // This version assumes periodic box.
            dx = x - xCent ;
            if(dx > BOX_WIDTH / 2) dx -= BOX_WIDTH ;
            if(dx < -BOX_WIDTH / 2) dx += BOX_WIDTH ;
            dy = y - yCent ;
            if(dy > BOX_WIDTH / 2) dy -= BOX_WIDTH ;
            if(dy < -BOX_WIDTH / 2) dy += BOX_WIDTH ;
            dz = z - zCent ;
            if(dz > BOX_WIDTH / 2) dz -= BOX_WIDTH ;
            if(dz < -BOX_WIDTH / 2) dz += BOX_WIDTH ;
            dx2 = dx * dx;
            dy2 = dy * dy;
            dz2 = dz * dz;
            rSquared = dx2 + dy2 + dz2 ;
            r = (float) Math.sqrt(rSquared) ;
            massRCubedInv = nParticles / (rSquared * r) ;
            a.x -= massRCubedInv * dx ;
            a.y -= massRCubedInv * dy ;
            a.z -= massRCubedInv * dz ;
        }
        
        int treeSize() {
            
            int size = 1 ;
            if (children != null) {         
                for(int n = 0 ; n < 8 ; n++) {
                    Node child = children [n] ;
                    if(child != null) {
                        size += child.treeSize() ;
                    }
                }
            }
            return size ;
        }
    }

    static class Vector {
        float x, y, z ;
        Vector() {}
        Vector(float x, float y, float z) {
            this.x = x ;
            this.y = y ;
            this.z = z ;
        }
    }
}

