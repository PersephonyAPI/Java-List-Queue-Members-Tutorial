/* 
 * AFTER RUNNING PROJECT WITH COMMAND: 
 * `gradle build && java -Dserver.port=0080 -jar build/libs/gs-spring-boot-0.1.0.jar`
 * 
*/

package main.java.list_queue_member;

import com.vailsys.persephony.api.PersyClient;
import com.vailsys.persephony.api.PersyException;
import com.vailsys.persephony.api.call.CallStatus;
import com.vailsys.persephony.api.queue.Queue;
import com.vailsys.persephony.api.queue.QueueCreateOptions;

import com.vailsys.persephony.percl.PerCLScript;
import com.vailsys.persephony.percl.Say;
import com.vailsys.persephony.percl.Language;
import com.vailsys.persephony.percl.Pause;
import com.vailsys.persephony.percl.Enqueue;

import com.vailsys.persephony.webhooks.application.ApplicationVoiceCallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import org.springframework.web.bind.annotation.RestController;

import com.vailsys.persephony.webhooks.queue.QueueWaitCallback;
import com.vailsys.persephony.webhooks.percl.GetDigitsActionCallback;
import com.vailsys.persephony.percl.GetDigits;
import com.vailsys.persephony.percl.GetDigitsNestable;
import com.vailsys.persephony.percl.Hangup;
import com.vailsys.persephony.percl.Dequeue;
import com.vailsys.persephony.webhooks.queue.QueueActionCallback;

import java.util.LinkedList;

import com.vailsys.persephony.api.queue.QueueList;

import java.util.ArrayList;
import com.vailsys.persephony.api.queue.member.Member;
import com.vailsys.persephony.api.queue.member.MemberList;

@RestController
public class ListQueueMemberController {
  // Get accountID and authToken from environment variables
  private String baseUrl = System.getenv("HOST");
  private String accountId = System.getenv("ACCOUNT_ID");
  private String authToken = System.getenv("AUTH_TOKEN");

  // To properly communicate with Persephony's API, set your Persephony app's
  // VoiceURL endpoint to '{yourApplicationURL}/InboundCall' for this example
  // Your Persephony app can be configured in the Persephony Dashboard
  @RequestMapping(value = {
      "/InboundCall" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> inboundCall(@RequestBody String request) {
    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();

    try {
      // Create a PersyClient object
      PersyClient client = new PersyClient(accountId, authToken);

      if (request != null) {
        // Convert the JSON into a request object
        ApplicationVoiceCallback callback = ApplicationVoiceCallback.createFromJson(request);

        // Verify inbound call is in the proper state
        if (callback.getCallStatus() == CallStatus.RINGING) {
          // Create PerCL say script with US English as the language
          Say say = new Say("Hello. Your call will be queued.");
          say.setLanguage(Language.ENGLISH_US);

          // Add PerCL say script to PerCL container
          script.add(say);

          // Create PerCL pause script with a 100 millisecond pause
          script.add(new Pause(100));

          // Create Queue options with an alias
          QueueCreateOptions options = new QueueCreateOptions();
          options.setAlias("InboundCallQueue");

          // Create a queue with an alias
          Queue queue = client.queues.create(options);
          // Create PerCL say to enqueue the call into the newly created queue with an
          // actionUrl
          Enqueue enqueue = new Enqueue(queue.getQueueId(), baseUrl + "/InboundCallAction");

          // Add waitUrl
          enqueue.setWaitUrl(baseUrl + "/InboundCallWait");

          // Add PerCL enqueue script to PerCL container
          script.add(enqueue);
        }
      }
    } catch (PersyException pe) {
      System.out.println(pe.getMessage());
    }

    // Convert PerCL container to JSON and append to response
    return new ResponseEntity<>(script.toJson(), HttpStatus.OK);
  }

  @RequestMapping(value = {
      "/InboundCallWait" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> inboundCallWait(@RequestBody String request) {
    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();
    System.out.println("Made it to InboundCallWait");
    if (request != null) {

      try {
        // Convert the JSON into a request object
        QueueWaitCallback.createFromJson(request);

        // Create PerCL getdigits script
        GetDigits digits = new GetDigits(baseUrl + "/CallDequeueSelect");

        // Create a list of prompts to use with the getdigits command
        LinkedList<GetDigitsNestable> prompts = new LinkedList<>();

        // Create PerCL say script with US English as the language
        Say say = new Say("Thank you for waiting. Press any key to exit queue.");
        say.setLanguage(Language.ENGLISH_US);

        // Add say script to the list of prompts
        prompts.add(say);

        // Set the list as the prompts to use with the getdigits command
        digits.setPrompts(prompts);

        // Add PerCL getdigits script to PerCL container
        script.add(digits);
      } catch (PersyException pe) {
        System.out.println(pe.getMessage());
      }
    }

    // Convert PerCL container to JSON and append to response
    return new ResponseEntity<>(script.toJson(), HttpStatus.OK);
  }

  @RequestMapping(value = {
      "/CallDequeueSelect" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> callDequeue(@RequestBody String request) {
    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();
    if (request != null) {
      try {
        // Convert JSON into a request object
        GetDigitsActionCallback callback = GetDigitsActionCallback.createFromJson(request);

        // Check if a digit was pressed
        if (callback.getDigits() != null && callback.getDigits().length() > 0) {
          // Create PerCL dequeue script and add to PerCL container
          script.add(new Dequeue());
        } else {
          // Create PerCl getdigits script
          GetDigits digits = new GetDigits(baseUrl + "/CallDequeueSelect");

          // Create a list of prompts to use with the getdigits command
          LinkedList<GetDigitsNestable> prompts = new LinkedList<>();

          // Create PerCL say script with US English as the language
          Say say = new Say("Thank you for waiting. Press any key to exit queue.");
          say.setLanguage(Language.ENGLISH_US);

          // Add say script to prompts list
          prompts.add(say);

          // Add prompts list to the getdigits command
          digits.setPrompts(prompts);

          // Add PerCL getdgitis script to PerCL container
          script.add(digits);
        }
      } catch (PersyException pe) {
        System.out.println(pe.getMessage());
      }
    }

    // Convert PerCL container to JSON and append to response
    return new ResponseEntity<>(script.toJson(), HttpStatus.OK);
  }

  @RequestMapping(value = {
      "/InboundCallAction" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> dequeueAction(@RequestBody String request) {
    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();
    System.out.println("Made it to InboundCallAction");
    if (request != null) {
      // Convert JSON into a request object
      try {
        QueueActionCallback.createFromJson(request);

        // Create PerCL say script with US English as the language
        Say say = new Say("Call exited queue.");
        say.setLanguage(Language.ENGLISH_US);

        // Add PerCL say script to PerCL container
        script.add(say);

        // Create and add PerCL hangup script to PerCL container
        script.add(new Hangup());
      } catch (PersyException pe) {
        System.out.println(pe.getMessage());
      }
    }

    // Convert PerCL container to JSON and append to response
    return new ResponseEntity<>(script.toJson(), HttpStatus.OK);
  }

  @RequestMapping("/queues")
  public ArrayList<Queue> listQueues() {

    try {
      PersyClient client = new PersyClient(accountId, authToken); // Create PersyClient object

      // Invoke get method to retrieve the first page of queues with a matching alias
      QueueList queueList = client.queues.get();

      // Check if the list is empty by checking its total size
      if (queueList.getTotalSize() > 0) {
        // Retrieve all pages of results
        while (queueList.getLocalSize() < queueList.getTotalSize()) {
          queueList.loadNextPage();
        }

        // Retrieve the inner ArrayList of queues to process
        ArrayList<Queue> queues = queueList.export();
        for (Queue queue : queues) {
          // Process queue element in some way
          System.out.println(queue);
        }
        return queues;
      }
    } catch (PersyException pe) {
      System.out.println(pe.getMessage());
    }

    return null;
  }

  @RequestMapping("/queueMembers")
  public ArrayList<Member> listQueueMembers(String queueId) {
    try {
      PersyClient client = new PersyClient(accountId, authToken); // Create PersyClient object

      // Invoke get method to retrieve initial list of queue member information
      MemberList memberList = client.queues.getMembersRequester(queueId).get();

      // Check if the list is empty by checking the total size
      if (memberList.getTotalSize() > 0) {
        // retrieve all queue member information from the server
        while (memberList.getLocalSize() < memberList.getTotalSize()) {
          memberList.loadNextPage();
        }

        // Create a list of the queue members
        ArrayList<Member> list = memberList.export();

        // Loop through the list to process queue member information
        for (Member member : list) {
          // Do some processing
          System.out.println(member);
        }

        return list;
      }
    } catch (PersyException pe) {
      System.out.println(pe.getMessage());
    }

    return null;
  }
}