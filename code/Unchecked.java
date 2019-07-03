import java.util.*;

public final class Unchecked {

  //void foo(List inputList) {
  //  List<String> list = (List<String>) inputList; // unsafe cast
  //}
  
   /**
    * Demonstrates -Xlint:cast warning of a redundant cast.
    */
   private static void demonstrateCastWarning()
   {
      final Set<String> people = new HashSet<String>();
      people.add("fred");
      people.add("wilma");
      people.add("barney");
      for (final String person : people)
      {
         // Redundant cast because generic type explicitly is String
         System.out.println("Person: " + ((String) person).toString());
      }
   }  
  
    public static void deprecateDate() {

        // Create a Date object for May 5, 1986
        // EXPECT DEPRECATION WARNING
        Date d = new Date(86, 04, 05);        // May 5, 1986
        System.out.println("Date is " + d);
    }  
  
  public static void main(String[] args){

    //List words = new ArrayList();
    //words.add("hello"); // this causes unchecked warning
    int x = 0 / 0;
    System.exit(0);
  }

}

//generic class, create array -- error