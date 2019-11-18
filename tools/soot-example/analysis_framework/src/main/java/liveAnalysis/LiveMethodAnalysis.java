package liveAnalysis;

import java.util.*;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

class LiveMethodAnalysis
        extends ForwardFlowAnalysis<Unit, FlowSet<AssignStmt>>
{

    private FlowSet<AssignStmt> emptySet;

    public LiveMethodAnalysis(DirectedGraph g) {
        // First obligation
        super(g);

        // Create the emptyset
        emptySet = new ArraySparseSet<AssignStmt>();

        // Second obligation
        doAnalysis();

    }


    // This method performs the joining of successor nodes
    // Since live variables is a may analysis we join by union
    @Override
    protected void merge(FlowSet<AssignStmt> inSet1,
                         FlowSet<AssignStmt> inSet2,
                         FlowSet<AssignStmt> outSet)
    {
        inSet1.union(inSet2, outSet);
    }


    @Override
    protected void copy(FlowSet<AssignStmt> srcSet,
                        FlowSet<AssignStmt> destSet)
    {
        srcSet.copy(destSet);
    }


    // Used to initialize the in and out sets for each node. In
    // our case we build up the sets as we go, so we initialize
    // with the empty set.
    @Override
    protected FlowSet<AssignStmt> newInitialFlow() {
        return emptySet.clone();
    }


    // Returns FlowSet representing the initial set of the entry
    // node. In our case the entry node is the last node and it
    // should contain the empty set.
    @Override
    protected FlowSet<AssignStmt> entryInitialFlow() {
        return emptySet.clone();
    }


    // Sets the outSet with the values that flow through the
    // node from the inSet based on reads/writes at the node
    // Set the outSet (entry) based on the inSet (exit)
    @Override
    protected void flowThrough(FlowSet<AssignStmt> inSet,
                               Unit node, FlowSet<AssignStmt> outSet) {

        outSet.clear();
        if (node instanceof AssignStmt) {
            boolean added = false;
            AssignStmt nodeStmt = (AssignStmt) node;
            AssignStmt clone;
            for (AssignStmt astmt: inSet) {
                if (astmt.getLeftOp().equals(nodeStmt.getLeftOp())) {
                    outSet.add((AssignStmt)nodeStmt.clone());
                    added = true;
                }
                else {
                    outSet.add(astmt);
                    //TODO differentiate normal static invokes from java static invokes
                    if (nodeStmt.getRightOp() instanceof StaticInvokeExpr && ((StaticInvokeExpr) nodeStmt.getRightOp()).getMethod().getDeclaringClass().getName().toLowerCase().startsWith("java.") && ((StaticInvokeExpr) nodeStmt.getRightOp()).getArgs().size() > 0 && ((StaticInvokeExpr) nodeStmt.getRightOp()).getArg(0).equals(astmt.getLeftOp())) {
                        clone = (AssignStmt) nodeStmt.clone();
                        clone.setRightOp(astmt.getRightOp());
                        outSet.add(clone);
                        added = true;
                    }
                    else if (nodeStmt.getRightOp() instanceof Constant && ((Value) nodeStmt.getRightOp()).equals(astmt.getLeftOp())) {
                        clone = (AssignStmt) nodeStmt.clone();
                        clone.setRightOp(astmt.getRightOp());
                        outSet.add(clone);
                        added = true;
                    }
                    else if (nodeStmt.getRightOp() instanceof Local && ((Value) nodeStmt.getRightOp()).equals(astmt.getLeftOp())) {
                        clone = (AssignStmt) nodeStmt.clone();
                        clone.setRightOp(astmt.getRightOp());
                        outSet.add(clone);
                        added = true;
                    }
                }
            }
            if (!added)
                outSet.add(nodeStmt);
        } else {
            inSet.copy(outSet);
        }


    }
}


