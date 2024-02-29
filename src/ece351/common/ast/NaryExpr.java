/* *********************************************************************
 * ECE351 
 * Department of Electrical and Computer Engineering 
 * University of Waterloo 
 * Term: Fall 2021 (1219)
 *
 * The base version of this file is the intellectual property of the
 * University of Waterloo. Redistribution is prohibited.
 *
 * By pushing changes to this file I affirm that I am the author of
 * all changes. I affirm that I have complied with the course
 * collaboration policy and have not plagiarized my work. 
 *
 * I understand that redistributing this file might expose me to
 * disciplinary action under UW Policy 71. I understand that Policy 71
 * allows for retroactive modification of my final grade in a course.
 * For example, if I post my solutions to these labs on GitHub after I
 * finish ECE351, and a future student plagiarizes them, then I too
 * could be found guilty of plagiarism. Consequently, my final grade
 * in ECE351 could be retroactively lowered. This might require that I
 * repeat ECE351, which in turn might delay my graduation.
 *
 * https://uwaterloo.ca/secretariat-general-counsel/policies-procedures-guidelines/policy-71
 * 
 * ********************************************************************/

package ece351.common.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.parboiled.common.ImmutableList;

import ece351.util.Examinable;
import ece351.util.Examiner;

/**
 * An expression with multiple children. Must be commutative.
 */
public abstract class NaryExpr extends Expr {

	public final ImmutableList<Expr> children;

	public NaryExpr(final Expr... exprs) {
		Arrays.sort(exprs);
		ImmutableList<Expr> c = ImmutableList.of();
		for (final Expr e : exprs) {
			c = c.append(e);
		}
    	this.children = c;
	}
	
	public NaryExpr(final List<Expr> children) {
		final ArrayList<Expr> a = new ArrayList<Expr>(children);
		Collections.sort(a);
		this.children = ImmutableList.copyOf(a);
	}

	/**
	 * Each subclass must implement this factory method to return
	 * a new object of its own type. 
	 */
	public abstract NaryExpr newNaryExpr(final List<Expr> children);

	/**
	 * Construct a new NaryExpr (of the appropriate subtype) with 
	 * one extra child.
	 * @param e the child to append
	 * @return a new NaryExpr
	 */
	public NaryExpr append(final Expr e) {
		return newNaryExpr(children.append(e));
	}

	/**
	 * Construct a new NaryExpr (of the appropriate subtype) with 
	 * the extra children.
	 * @param list the children to append
	 * @return a new NaryExpr
	 */
	public NaryExpr appendAll(final List<Expr> list) {
		final List<Expr> a = new ArrayList<Expr>(children.size() + list.size());
		a.addAll(children);
		a.addAll(list);
		return newNaryExpr(a);
	}

	/**
	 * Check the representation invariants.
	 */
	public boolean repOk() {
		// programming sanity
		assert this.children != null;
		// should not have a single child: indicates a bug in simplification
		assert this.children.size() > 1 : "should have more than one child, probably a bug in simplification";
		// check that children is sorted
		int i = 0;
		for (int j = 1; j < this.children.size(); i++, j++) {
			final Expr x = this.children.get(i);
			assert x != null : "null children not allowed in NaryExpr";
			final Expr y = this.children.get(j);
			assert y != null : "null children not allowed in NaryExpr";
			assert x.compareTo(y) <= 0 : "NaryExpr.children must be sorted";
		}
        // Note: children might contain duplicates --- not checking for that
        // ... maybe should check for duplicate children ...
		// no problems found
		return true;
	}

	/**
	 * The name of the operator represented by the subclass.
	 * To be implemented by each subclass.
	 */
	public abstract String operator();
	
	/**
	 * The complementary operation: NaryAnd returns NaryOr, and vice versa.
	 */
	abstract protected Class<? extends NaryExpr> getThatClass();
	

	/**
     * e op x = e for absorbing element e and operator op.
     * @return
     */
	public abstract ConstantExpr getAbsorbingElement();

    /**
     * e op x = x for identity element e and operator op.
     * @return
     */
	public abstract ConstantExpr getIdentityElement();


	@Override 
    public final String toString() {
    	final StringBuilder b = new StringBuilder();
    	b.append("(");
    	int count = 0;
    	for (final Expr c : children) {
    		b.append(c);
    		if (++count  < children.size()) {
    			b.append(" ");
    			b.append(operator());
    			b.append(" ");
    		}
    		
    	}
    	b.append(")");
    	return b.toString();
    }


	@Override
	public final int hashCode() {
		return 17 + children.hashCode();
	}

	@Override
	public final boolean equals(final Object obj) {
		if (!(obj instanceof Examinable)) return false;
		return examine(Examiner.Equals, (Examinable)obj);
	}
	
	@Override
	public final boolean isomorphic(final Examinable obj) {
		return examine(Examiner.Isomorphic, obj);
	}
	
	private boolean examine(final Examiner e, final Examinable obj) {
		// basics
		if (obj == null) return false;
		if (!this.getClass().equals(obj.getClass())) return false;
		final NaryExpr that = (NaryExpr) obj;
		
		// if the number of children are different, consider them not equivalent
		// since the n-ary expressions have the same number of children and they are sorted, just iterate and check
		// supposed to be sorted, but might not be (because repOk might not pass)
		// if they are not the same elements in the same order return false
		// no significant differences found, return true
		
		// TODO: longer code snippet
		if (that.children.size() != this.children.size()) {
			return false;
		}

		// Use a for loop to check if any corresponding children are not equal
		for (int i = 0; i < this.children.size(); i++) {
			if (!this.children.get(i).equals(that.children.get(i))) {
				return false;
			}
		}

		// If the loop completes without returning false, all children are equal
		return true;


	}

	
	@Override
	protected final Expr simplifyOnce() {
		assert repOk();
		final Expr result = 
				simplifyChildren().
				mergeGrandchildren().
				foldIdentityElements().
				foldAbsorbingElements().
				foldComplements().
				removeDuplicates().
				simpleAbsorption().
				subsetAbsorption().
				singletonify();
		assert result.repOk();
		return result;
	}
	
	/**
	 * Call simplify() on each of the children.
	 */
	private NaryExpr simplifyChildren() {
		// note: we do not assert repOk() here because the rep might not be ok
		// the result might contain duplicate children, and the children
		// might be out of order

		//storing the updated list of the childs simplified
		ArrayList<Expr> localList = new ArrayList<>();
		//go through all the 
		for (Expr child : this.children) {
			Expr simplifiedChild = child.simplify();
			localList.add(simplifiedChild);
		}


		return newNaryExpr(localList);
	}

	
	private NaryExpr mergeGrandchildren() {
		// extract children to merge using filter (because they are the same type as us)
			// if no children to merge, then return this (i.e., no change)
			// use filter to get the other children, which will be kept in the result unchanged
			// merge in the grandchildren
			// assert result.repOk():  this operation should always leave the AST in a legal state

		//find all the elements that are the same as out current expr

		NaryExpr sameTypeChildren = this.filter(this.getClass(), true);

		//if no chnage is needed
		if(sameTypeChildren.children.size() == 0) return this;

		NaryExpr differentTypeChildren = this.filter(this.getClass(), false);

		//updated list with the removal of the parent nodes with the same value as the head node
		ArrayList<Expr> newChildren = new ArrayList<>(differentTypeChildren.children);

		for (final Expr child : sameTypeChildren.children) {
			if (child instanceof NaryExpr) {
				//loop through all the 
				for (Expr grandchild : ((NaryExpr) child).children) {
					newChildren.add(grandchild);
				}
			}
		}

		return newNaryExpr(newChildren);
	}


    private NaryExpr foldIdentityElements() {
    	// if we have only one child stop now and return self
    	// we have multiple children, remove the identity elements
    		// all children were identity elements, so now our working list is empty
    		// return a new list with a single identity element
    		// normal return

		if(children.size() != 0){
			ArrayList<Expr> idenArrayList = new ArrayList<>();

			//go through all the childredn and find the
			idenArrayList.add(this.getIdentityElement());

			//remove all the instances of identity elements
			NaryExpr res = removeAll(idenArrayList, Examiner.Equals);


			if(res.children.size() == 0){
				return res.append(this.getIdentityElement());
			}

			else{
				return res;
			}

		}

		else{
			return this;
		}
    	// do not assert repOk(): this fold might leave the AST in an illegal state (with only one child)
    }

    private NaryExpr foldAbsorbingElements() {
		// absorbing element: 0.x=0 and 1+x=1
			// absorbing element is present: return it
			// not so fast! what is the return type of this method? why does it have to be that way?

			//if we have the element then we add to the child list
			if(this.contains(this.getAbsorbingElement(), Examiner.Equals)){
				ArrayList<Expr> idenArrayList = new ArrayList<>();
				idenArrayList.add(getAbsorbingElement());
				return newNaryExpr(idenArrayList);

			}
			// no absorbing element present, do nothing
		return this; // TODO: replace this stub
    	// do not assert repOk(): this fold might leave the AST in an illegal state (with only one child)
	}

	private NaryExpr foldComplements() {

		//filter out all the negations
		NaryExpr complement = this.filter(NotExpr.class, true);

		// Iterate over the negations and try to see if its complement is in our children list
		for (Expr n : complement.children	) {
			// found matching negation and its complement
			if (this.contains(((NotExpr)n).expr, Examiner.Equals)) {
				// return absorbing element
				return newNaryExpr(Arrays.asList(this.getAbsorbingElement()));
			}
		}
		
		// No complements to fold, return the original expression
		return this;
	}
	

	private NaryExpr removeDuplicates() {
		// remove duplicate children: x.x=x and x+x=x
		// since children are sorted this is fairly easy
			// no changes
			// removed some duplicates
		// If there's only one or no child, no duplicates can exist
		if (this.children.size() <= 1) {
			return this;
		}
	
		// List to store unique children
		List<Expr> uniqueChildren = new ArrayList<>();
	
		// Add the first child to start comparisons
		uniqueChildren.add(this.children.get(0));
	
		// Iterate over the children, starting from the second child
		for (int i = 1; i < this.children.size(); i++) {
			Expr currentChild = this.children.get(i);
			Expr lastUniqueChild = uniqueChildren.get(uniqueChildren.size() - 1);
	
			// If the current child is different from the last unique child, add it to the unique list
			if (!currentChild.equals(lastUniqueChild)) {
				uniqueChildren.add(currentChild);
			}
		}
	
		// Return a new NaryExpr with the unique children
		return newNaryExpr(uniqueChildren);
    	// do not assert repOk(): this fold might leave the AST in an illegal state (with only one child)
	}

	private NaryExpr simpleAbsorption() {
		// (x.y) + x ... = x ...
		// check if there are any conjunctions that can be removed
		
		//find all the elements that are the same type as this
		NaryExpr sameType = this.filter(getThatClass(), false);

		//find all the elements that are of the opposite type as this
		NaryExpr differentType = this.filter(getThatClass(), true);

		//this list will contain all the elements that we can remove
		List<Expr> remove = new ArrayList<>();

		//check to see if we have differentType to loop over
		if(!differentType.children.isEmpty()){
			//go through the expressions in the opposite type array 
			for(Expr diffExpr: differentType.children){
				//loop the child elements in the the expr
				for(Expr x : ((NaryExpr)diffExpr).children){
					//if we have this child element in any of the expr in the same type list
					if(sameType.contains(x, Examiner.Equals)){
						//remove this expression from the children of this
						remove.add(diffExpr);
					}
				}
	
			}
			
			
		}
		return this.removeAll(remove, Examiner.Equals);
	}

	private NaryExpr subsetAbsorption() {
		Set<Expr> exprSet = new HashSet<>();
	
		// Handle case 1 grouped collection: ( a + b ) + ( ( a + b ) * y )
		for (int i = 0; i < this.children.size(); i++) {
			Expr child1 = this.children.get(i);
			if (child1 instanceof NaryExpr) {
				NaryExpr naryChild1 = (NaryExpr) child1;
	
				for (int j = 0; j < this.children.size(); j++) {
					if (i != j) {
						Expr child2 = this.children.get(j);
						if (child2 instanceof NaryExpr) {
							NaryExpr naryChild2 = (NaryExpr) child2;
	
							// Check if one is a superset of the other and add to set
							if (naryChild1.children.containsAll(naryChild2.children)) {
								exprSet.add(naryChild1);
							}
						}
					}
				}
			}
		}
	
		// Handle case 2 loose collection: a + b + ( ( a + b ) * y )
		NaryExpr first_children_parent = this.filter(getThatClass(), true);
		if (!first_children_parent.children.isEmpty()) {
			for (Expr e1 : first_children_parent.children) {
				if (e1 instanceof NaryExpr) {
					for (Expr e2 : ((NaryExpr) e1).children) {
						if (e2 instanceof NaryExpr && this.children.containsAll(((NaryExpr) e2).children)) {
							exprSet.add(e1);
						}
					}
				}
			}
		}
	
		// Remove all the identified expressions
		return this.removeAll(new ArrayList<>(exprSet), Examiner.Equals);
	}
	
	
	


	/**
	 * If there is only one child, return it (the containing NaryExpr is unnecessary).
	 */
	private Expr singletonify() {
		// if we have only one child, return it
		// having only one child is an illegal state for an NaryExpr
			// multiple children; nothing to do; return self

		if(children.size() == 1){
			return (this.children.get(0));
		}
		else{

			return this;
		}
	}

	/**
	 * Return a new NaryExpr with only the children of a certain type, 
	 * or excluding children of a certain type.
	 * @param filter
	 * @param shouldMatchFilter
	 * @return
	 */
	public final NaryExpr filter(final Class<? extends Expr> filter, final boolean shouldMatchFilter) {
		ImmutableList<Expr> l = ImmutableList.of();
		for (final Expr child : children) {
			if (child.getClass().equals(filter)) {
				if (shouldMatchFilter) {
					l = l.append(child);
				}
			} else {
				if (!shouldMatchFilter) {
					l = l.append(child);
				}
			}
		}
		return newNaryExpr(l);
	}

	public final NaryExpr filter(final Expr filter, final Examiner examiner, final boolean shouldMatchFilter) {
		ImmutableList<Expr> l = ImmutableList.of();
		for (final Expr child : children) {
			if (examiner.examine(child, filter)) {
				if (shouldMatchFilter) {
					l = l.append(child);
				}
			} else {
				if (!shouldMatchFilter) {
					l = l.append(child);
				}
			}
		}
		return newNaryExpr(l);
	}

	public final NaryExpr removeAll(final List<Expr> toRemove, final Examiner examiner) {
		NaryExpr result = this;
		for (final Expr e : toRemove) {
			result = result.filter(e, examiner, false);
		}
		return result;
	}

	public final boolean contains(final Expr expr, final Examiner examiner) {
		for (final Expr child : children) {
			if (examiner.examine(child, expr)) {
				return true;
			}
		}
		return false;
	}

}
