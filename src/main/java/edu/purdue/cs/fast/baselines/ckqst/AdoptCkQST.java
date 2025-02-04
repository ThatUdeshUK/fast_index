package edu.purdue.cs.fast.baselines.ckqst;

import edu.purdue.cs.fast.SpatialKeywordIndex;
import edu.purdue.cs.fast.baselines.ckqst.structures.IQuadTree;
import edu.purdue.cs.fast.baselines.fast.LFAST;
import edu.purdue.cs.fast.baselines.fast.messages.LDataObject;
import edu.purdue.cs.fast.baselines.fast.messages.LMinimalRangeQuery;
import edu.purdue.cs.fast.helper.SpatialHelper;
import edu.purdue.cs.fast.models.DataObject;
import edu.purdue.cs.fast.models.Query;
import edu.purdue.cs.fast.models.Rectangle;
import edu.purdue.cs.fast.models.KNNQuery.EuclideanComparator;
import edu.purdue.cs.fast.structures.BoundedPriorityQueue;

import java.util.Collection;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * Adopts CkQST framework with FAST as the query index.
 */
public class AdoptCkQST implements SpatialKeywordIndex<Query, DataObject> {
    public static int xRange = 10;
    public static int yRange = 10;
    public static int maxHeight = 9;
    public static int maxLeafCapacity = 5;

    public static double thetaU;
    public final IQuadTree objectIndex;
    public final LFAST queryIndex;
    public final HashMap<Integer, BoundedPriorityQueue<DataObject>> resultList;
    private int timestamp = 0;

    public AdoptCkQST(int xRange, int yRange, int maxHeight) {
        AdoptCkQST.xRange = xRange;
        AdoptCkQST.yRange = yRange;
        AdoptCkQST.maxHeight = maxHeight;
        objectIndex = new IQuadTree(0, 0, xRange, yRange, maxLeafCapacity, maxHeight);
        queryIndex = new LFAST(new Rectangle(0, 0, xRange, yRange), 512, 9);
        resultList = new HashMap<>();
        thetaU = 0.5;
    }

    @Override
    public void preloadObject(DataObject object) {
        objectIndex.insert(object);
    }

    @Override
    public Collection<DataObject> insertQuery(Query query) {
        timestamp++;
        if (query.getClass() == LMinimalRangeQuery.class) {
            PriorityQueue<DataObject> objResults = (PriorityQueue<DataObject>) objectIndex.search(query);

            if (objResults.size() >= ((LMinimalRangeQuery) query).k) {
                DataObject o = objResults.peek();
                assert o != null;
                double sr = SpatialHelper.getDistanceInBetween(((LMinimalRangeQuery) query).location, o.location);
                ((LMinimalRangeQuery) query).update(sr);

                
            }

            if (!resultList.containsKey(query.id)) {
                BoundedPriorityQueue<DataObject> newList = new BoundedPriorityQueue<>(((LMinimalRangeQuery) query).k, new EuclideanComparator(((LMinimalRangeQuery) query).location));
                for (DataObject obj : objResults) {
                    newList.add(obj);
                }
                resultList.put(query.id, newList);
            }

            queryIndex.insertQuery((LMinimalRangeQuery) query);
        } else
            throw new RuntimeException("CkQST only support KNNQueries!");
        return null;
    }

    @Override
    public Collection<Query> insertObject(DataObject dataObject) {
        timestamp++;

        objectIndex.insert(dataObject);
        Collection<Query> queryResults = queryIndex.insertObject((LDataObject) dataObject);
        for (Query query : queryResults) {
            if (query instanceof LMinimalRangeQuery) {
                BoundedPriorityQueue<DataObject> rList = resultList.get(query.id);
                rList.add(dataObject);
                boolean kStarFilled = rList.isFull();
                if (kStarFilled) {
                    assert rList.peek() != null;
        
                    DataObject o = rList.peek();
                    double sr = SpatialHelper.getDistanceInBetween(((LMinimalRangeQuery) query).location, o.location);
                    ((LMinimalRangeQuery) query).update(sr);
                } else {
                    PriorityQueue<DataObject> objResults = (PriorityQueue<DataObject>) objectIndex.search(query);
                    
                    for (DataObject obj : objResults) {
                        rList.add(obj);
                    }
                    
                    DataObject o = rList.peek();
                    double sr = SpatialHelper.getDistanceInBetween(((LMinimalRangeQuery) query).location, o.location);
                    ((LMinimalRangeQuery) query).update(sr);
                    // if (objResults.size() >= ((LMinimalRangeQuery) query).k) {
                    //     DataObject o = objResults.peek();
                    //     assert o != null;
                    //     double sr = SpatialHelper.getDistanceInBetween(((LMinimalRangeQuery) query).location, o.location);
                    //     ((LMinimalRangeQuery) query).update(sr);
                    // }
                }
            }
        }
        return queryResults;
    }

    public void printIndex() {
        System.out.println(queryIndex);
    }
}
