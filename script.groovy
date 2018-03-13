//Thread Pool Includes
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.Future

//Time include
import groovy.time.*

//Overload function  Calls Execute on shell using the current directory.
private def executeOnShell(String unit, String command) {
  return executeOnShell(unit, command, new File(System.properties.'user.dir'))
}

//Executes a command
//  Unit is the Object of the command.
//  Command is the execution command.
//  This adds the shell prefix for execution and records the time.
//  If there is an error it will be printed and a file with the entire
//  command output will be generated.
private def executeOnShell(String unit, String command, File workingDir) {
  println "Starting " + unit
  def startTime = new Date()
  
  def file = new File(unit + ".error." + scriptStartTime.format("yyyyMMdd_HHmmss") + ".log")
  if(file.exists()){file.delete()}
  file << unit + "\n"
  file << command + "\n"	
  
  def process = new ProcessBuilder(addShellPrefix(command))
                                    .directory(workingDir)
                                    .redirectErrorStream(true) 
                                    .start()
  //need to write the input stream live.  Seems to choke otherwise on processes with a lot of stdout
  process.inputStream.eachLine {file << it << "\n"}
  process.waitFor();
	
  //See How long the command took
  def endTime = new Date()
  TimeDuration duration = TimeCategory.minus(endTime, startTime)
  println "Finished " + unit + " Time: " + duration
  //If the process failed, Save the processes output.
  if(process.exitValue() != 0)
  {
    file << "Return Code: " + process.exitValue() + "\n"    
    file << "\n"
    println "See " + file.getAbsolutePath() + " for more details."
  }else{
    //Delete the file, no need to save it process executed successfully
    file.deleteOnExit();
  }
	
  return process.exitValue();

}

//adds the shell prefix to the command path.  This helps with wildcards amoung other things.
private def addShellPrefix(String command) {
  commandArray = new String[3]
  commandArray[0] = "sh"
  commandArray[1] = "-c"
  commandArray[2] = command
  return commandArray
}

//Start our Scripting
//Create a thread pool with x threads
def threadPool = Executors.newFixedThreadPool(2)

//Define a map of commands to run
def cmds = [:]
cmds.put("cmd1", "~/scripts/sleep3.sh")
cmds.put("cmd2", "~/scripts/sleep5.sh")
cmds.put("cmd3", "~/scripts/sleep3.sh")
cmds.put("cmd4", "~/scripts/sleep5.sh")
cmds.put("cmd5", "~/scripts/sleep3.sh")
cmds.put("cmd6", "~/scripts/error.sh")

println "Starting Execution of Tasks"

//Global Start Time
scriptStartTime = new Date()

println scriptStartTime.format("yyyyMMdd-HH:mm:ss.SSS")

def returnCode = 0

try {

  //Take the list of commands and and send them to the thread pool to be processed.
  List<Future> futures = cmds.collect{entry->
    threadPool.submit({->
    executeOnShell(entry.key, entry.value) } as Callable);
  }

  // recommended to use following statement to ensure the execution of all tasks.
  //  Holds until each future is available.
  futures.each{
        // If return Code wasn't 0 print it and make that the return code of this script
        if(0 != it.get()) {
	  println "return code: " + it.get()
	  returnCode = it.get()
        }
  }
}finally {
  threadPool.shutdown()
  System.exit(returnCode)
}


