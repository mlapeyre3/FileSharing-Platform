/* Remarks
	
- About the error case in ajax requests -
Handling error in a graceful way is already been done in the loading part
because when the timeout expires then it triggers a function error.
Here we just log the error in the browser javascript console.

- About downloading files -
By default files are identified as text documents and are proposed to be open in a text editor.
Indeed, as files are retrieved from base64 string then we do not know what types they are.

- About uploading a file -
The magic piece of code converting a file to a base64 string was not obvious to produce
We were strongly helped by the smart jsfiddle at https://jsfiddle.net/eliseosoto/JHQnk/

End of remarks */


$(document).ready(function() {
	
	/* * * * * * * * * * * * * * *
	 *    Global variables       * 
	 * * * * * * * * * * * * * * */

	/* Contact the server */
	var protocol_server = "http";
	var ip_server = "104.196.55.208";
	var port_server = "8080";
	var url_server = protocol_server + "://" + ip_server + ":" + port_server + "/";
	var max_timeout = 100000;
	
	/* Select a group on the 'home' page */
	var current_group_id = "";
	var current_group_name = "";
	
	/* Select a file on the 'group' page */
	current_file_special_ip = ""
	var current_file_id = "";
	var current_file_name = "";
	
	/* Add a user on the 'group' page */
	var counter_add_user_group = 1;
	
	/* Add a user on the 'creategroup' page */
	var counter_add_user_creategroup = 1;
	
	/* token variable */
	var token = readCookie("token");
	//var token = "test";
	
	/* * * * * * * * * * * * * * *
	 *  end of global variables  * 
	 * * * * * * * * * * * * * * */
	

	/* * * * * * * * * * * * * * *
	 *        'home' page       * 
	 * * * * * * * * * * * * * * */
	
	/* set the 'home' page
	queries the secure share servers to retrieve the groups the user belongs to display those groups */
	$("#home").on("pagebeforecreate", function() {
		
		/* first clean up the page and related variables */
		$("#loading").empty();
		$("#displaygroups").empty();
		
		current_group_id = "";
		current_group_id = "";
		current_group_name = "";
		
		/* query the server */
		$.ajax({
			/* set the post request to the '/displaygroup' url */
			type: "POST",
			url: url_server + "test/displaygroups",
			headers: { 'Content-Type': 'application/json' },
			headers: { 'Access-Control-Allow-Origin': '*' },
			data: '{ "token" : "' + token + '" }',
			dataType: "json",
			
			/* display loading image */
			beforeSend: function(){
				$("#loading").append('<img alt="loading..." src="./img/loading.gif" width="200px"/><br/><span>Please wait while contacting the server...</span>').trigger("create");
			},
			complete: function(){
				$("#loading").empty();
			},
			
			/* wait for some time before deciding the server is down */
			timeout: max_timeout,
			
			/* answer from the server : success */
			success: function(data)
			{
				$("#displaygroups").append('<h1>Groups</h1>');
				
				$.each(data.groups, function(row, content) {
					$("#displaygroups").append('<li><a class="viewgroup" href="#group" id="' + content.id + '">' + content.name + '</a></li>');
				});
			},
			
			/* answer from the server : error
			or no answer from the server */
			error: function(data)
			{	
				console.log("error message :" + JSON.parse(data.responseText).error + "\nerror type : " + data.error_type);
			}
		})
		
		/* answer from the server : error
		or no answer from the server */
		.fail(function(data)
		{
			if (JSON.parse(data.responseText).error.indexOf("consistent") != -1)
			{
				$("#displaygroups").append('<span>You do not belong to any group yet</span>');
			}
			else {
				$("#displaygroups").append('<img alt=\"Oooops!\" src=\"./img/failure.png\" width=\"200px\"/><br/><span>An error occured while contacting the server</span>');
			}
		});
	});
	
	/* keep track of the group selected once clicked onto on the 'home' page */
	$("#displaygroups").on("click", ".viewgroup", function() {
		current_group_id = $(this).attr('id');
		current_group_name = $(this).html();
	});
	
	/* * * * * * * * * * * * * * *
	 *     end of 'home' page    * 
	 * * * * * * * * * * * * * * */
	
	
	/* * * * * * * * * * * * * * *
	 *        'group' page       * 
	 * * * * * * * * * * * * * * */
	
	/* * * display the files on the group * * */
	
	/* set the 'group' page board
	queries secure share servers to display files related to this group
	queries secure share servers to precise displayed files' last versions
	reset the 'add user' form */
	$("#group").on("pagebeforecreate", function() {
		
		/* first clean up the page and related variables */
		$("#group_title").empty();
		$('#group_title').append("<h1>Group '" + current_group_name + "' </h1>");
		$("#loading").empty();
		$("#displayfiles").empty();
		$("#displayusers").empty();
		
		counter_add_user_group = 1;
		
		/* query the server
		display files related to the group */
		$.ajax({
			/* set the post request to the '/displayfiles' url */
			type: "POST",
			url: url_server + "test/displayfiles",
			data: '{ "token" : "' + token + '", ' + '"group_id" : "' + current_group_id + '"}',
			dataType: "json",
			
			/* display loading image */
			beforeSend: function(){
				$("#loading").append('<img alt="loading..." src="./img/loading.gif" width="200px"/><br/><span>Please wait while contacting the server...</span>').trigger("create");
			},
			complete: function(){
				$("#loading").empty();
			},
			
			/* wait for some time before deciding the server is down */
			timeout: max_timeout,
			
			/* answer from the server : success */
			success: function(data)
			{
				$.each(data.files_list, function(row, content) {					
					$('#displayfiles').append('<li><a class="viewfile" id="' + content.id + '" >' + content.name + '</a></li>');
				});
			},
			
			/* answer from the server : error
			or no answer from the server */
			error: function(data)
			{
				console.log("error message :" + JSON.parse(data.responseText).error + "\nerror type : " + data.error_type);
			}
		})
		
		/* answer from the server : error
		or no answer from the server */
		.fail(function(data)
		{
			if (JSON.parse(data.responseText).error.indexOf("consistent") != -1)
			{
				$("#displayfiles").append('<span>No files in the group</span>');
			}
			else {
				$("#displayfiles").append('<img alt=\"Oooops!\" src=\"./img/failure.png\" width=\"200px\"/><br/><span>An error occured while contacting the server</span>');
			}
		});
		
		/* donwload the file selected once clicked onto on the 'group' page */
		$("#displayfiles").on("click", ".viewfile", function() {

			/* keep track of the file selected once clicked onto on the 'group' page */
			current_file_id = $(this).attr("id").split(":")[0];
			current_file_name = $(this).html();
		
			/* query the server
			get the IP address of the server having authority for the particular file */
			$.ajax({
				/* set the post request to the '/downloadfileinfos' url */
				type: "POST",
				url: url_server + "test/downloadfileinfos",
				data: '{ "token" : "' + token + '", "group_id": "' + current_group_id + '", "file_id": "' + current_file_id + '" }',
				dataType: "json",
				
				/* wait for some time before deciding the server is down */
				timeout: max_timeout,
				
				/* answer from the server : success */
				success: function(data)
				{
					current_file_special_ip = data.server;
		
					/* query the server
					download the file clicked onto */
					$.ajax({
						/* set the post request to the '/getfile' url */
						type: "POST",
						url: url_server + "test/getfile",
						data: '{ "token" : "' + token + '", "group_id": "' + current_group_id + '", "file_id": "' + current_file_id + '", "IP" : "' + current_file_special_ip + '" }',
						dataType: "json",
						
						/* display loading image */
						beforeSend: function(){
							$("#loading").append('<img alt="loading..." src="./img/loading.gif" width="200px"/><br/><span>Please wait while contacting the server...</span>').trigger("create");
						},
						complete: function(){
							$("#loading").empty();
						},
						
						/* wait for some time before deciding the server is down */
						timeout: max_timeout,
						
						/* answer from the server : success */
						success: function(data)
						{
							/* decode the base64 string retrieved from the server */
							var file = window.atob((data.file.substr(data.file.indexOf('/') + 1, data.file.length)));
							
							/* generate local URI from retrieved base64 string */
							uri = "data:text;charset=utf-8," + file;
							
							/* generate an hyperlink and click on it so that user can download the file
							then immediately delete traces of that temporary hyperlink */
							var downloadLink = document.createElement("a");	// define the <a> element
							downloadLink.href = uri;						// make it point to the file uri
							downloadLink.download = current_file_name;		// set it with the name of the downloaded file
							document.body.appendChild(downloadLink);		// instantiate this element
							downloadLink.click();							// click on this element
							document.body.removeChild(downloadLink);		// remove this element
						},
						
						/* answer from the server : error
						or no answer from the server */
						error: function(data)
						{
							console.log("error message :" + JSON.parse(data.responseText).error + "\nerror type : " + data.error_type);
						}
					})
					
					/* answer from the server : error
					or no answer from the server */
					.fail(function()
					{
						alert("There was an error when downloading the file from the server");
					});
				},
	
				/* answer from the server : error
				or no answer from the server */
				error: function(data)
				{
					console.log("error message :" + JSON.parse(data.responseText).error + "\nerror type : " + data.error_type);
				}
			})
			
			/* answer from the server : error
			or no answer from the server */
			.fail(function()
			{
				$('#displayfiles').empty();
				$('#displayfiles').append("<img alt=\"Oooops!\" src=\"./img/failure.png\" width=\"200px\"/><br/><span>An error occured while contacting the server</span>");
			});
		});
		
		/* * * end of display the files on the group * * */
		
		
		/* * * display the members of the group * * */
		
		/* query the server
		display users belonging to the group */
		$.ajax({
			/* set the post request to the '/displayusers' url */
			type: "POST",
			url: url_server + "test/displayusers",
			data: '{ "token" : "' + token + '", "group_id": "' + current_group_id + '" }',
			dataType: "json",
			
			/* display loading image */
			beforeSend: function(){
				$("#loading").append('<img alt="loading..." src="./img/loading.gif" width="200px"/><br/><span>Please wait while contacting the server...</span>').trigger("create");
			},
			complete: function(){
				$("#loading").empty();
			},
			
			/* wait for some time before deciding the server is down */
			timeout: max_timeout,
			
			/* answer from the server : success */
			success: function(data)
			{
				$.each(data.users_list, function(row, content) {
					$("#displayusers").append('<li> ' + content + '</li>');
				});
			},
			
			/* answer from the server : error
			or no answer from the server */
			error: function(data)
			{
				console.log("error message :" + JSON.parse(data.responseText).error + "\nerror type : " + data.error_type);
			}
		})
		
		/* answer from the server : error
		or no answer from the server */
		.fail(function()
		{
			$('#displayusers').append("<img alt=\"Oooops!\" src=\"./img/failure.png\" width=\"200px\"/><br/><span>An error occured while contacting the server</span>");
		});
	});
	
	/* * * end of display the members of the group * * */
	
	
	/* * * upload a file * * */
	
	/* convert a file to a base64 string */
	var file_upload_string = "";
	
	var handleFileSelect = function(evt) {						// define a event handler function
		var files = evt.target.files;							// retrieve all files
		var file = files[0];									// get the first file of the previous list
		if (files && file) {									// if everything is ok so far...
			var reader = new FileReader();						// ...define a file reader...
			reader.onload = function(readerEvt) {				// ...and when reading the file...
				var binaryString = readerEvt.target.result;		// ...get the binary data being read...
				file_upload_string = btoa(binaryString);		// ...and base64 decode it
			};
			reader.readAsBinaryString(file);					// instantiate the reading of the file
		}
	};
	
	if (window.File && window.FileReader && window.FileList && window.Blob) {						// if all necessary javascript functions are available
		var tag = document.getElementById("upload_file");
		document.getElementById("upload_file").addEventListener("change", handleFileSelect, false);	// then cast the event handler function on the uploading field
	}
	else {
		alert("Your browser cannot upload files...");												// otherwise alert user that file cannot be uploaded
	}
	
	/* upload the file once clicked onto the 'upload' button */
	$("#upload").on("click", function() {
		
		file_upload_string = document.getElementById("upload_file").value.replace("C:\\fakepath\\", "") + "/" + file_upload_string;	// add the filename at the beginning of the string
		
		/* query the server
		upload the file loaded in the browser */
		$.ajax({
			/* set the post request to the '/uploadfile' url */
			type: "POST",
			url: url_server + "test/uploadfile",
			data: '{ "token" : "' + token + '", ' + '"group_id" : "' + current_group_id + '" , "file" : "' + file_upload_string + '"}',
			dataType: "json",
			
			/* display loading image */
			beforeSend: function(){
				$("#loading").append('<img alt="loading..." src="./img/loading.gif" width="200px"/><br/><span>Please wait while contacting the server...</span>').trigger("create");
			},
			complete: function(){
				$("#loading").empty();
			},
			
			/* wait for some time before deciding the server is down */
			timeout: max_timeout,
			
			/* answer from the server : success */
			success: function(data)
			{		
				/* alert the user that the file has been successfully uploaded */
				alert('The file has been successfully uploaded');
				
				/* reset the uploading form */
				$("#form_upload_file").empty();
				$("#form_upload_file").append('<input type="file" id="upload_file" />').trigger("create");
			},
			
			/* answer from the server : error
			or no answer from the server */
			error: function(data)
			{
				console.log("error message :" + JSON.parse(data.responseText).error + "\nerror type : " + data.error_type);
			},
		})
		
		/* answer from the server : error
		or no answer from the server */// no answer from the server : fail
		.fail(function()
		{
			alert("An error occured when uploading the file");
		});
	});
	
	/* * * end of upload a file * * */
	
	
	/* * * register new user * * */
	
	/* add a user field in the form on the 'group' page */
	$("#adduser").on("click", function() {
		counter_add_user_group += 1;
		$("#form_adduser").append('<input id="user_mail' + counter_add_user_group + '" type="text" placeholder="User email #' + counter_add_user_group + '" />' ).trigger("create");
	});
	
	/* register users to a group on the 'group' page */
	$("#submit_add_users").on("click", function() {
		
		/* retrieve members the user wants to add to the group */
		var members = '[';
		var inputs = document.getElementById("form_adduser").elements;
		for (index = 0; index < inputs.length; ++index) {
			if (index != (inputs.length - 1)) {
				members = members + ' "' + inputs[index].value + '",';
			}
			else{
				members = members + ' "' + inputs[index].value + '" ]';
			}
		}
		
		/* query the server
		add users to an existing group */
		$.ajax({
			/* set the post request to the '/addusers' url */
			type: "POST",
			url: url_server + "test/addusers",
			data: '{ "token" : "' + token + '", "group_id" : "' + current_group_id + '" , "members" : ' + members + '}',
			dataType: "json",
			
			/* display loading image */
			beforeSend: function(){
				$("#loading").append('<img alt="loading..." src="./img/loading.gif" width="200px"/><br/><span>Please wait while contacting the server...</span>').trigger("create");
			},
			complete: function(){
				$("#loading").empty();
			},
			
			/* wait for some time before deciding the server is down */
			timeout: max_timeout,
			
			/* answer from the server : success */
			success: function(data)
			{
				if (data.status == "OK") {
					
					/* alert the user that the users have been successfully registered */
					alert("Users have been correctly added to the group");
					
					/* reset the 'add user' form on the 'group' page */
					$("#form_adduser").empty();
					$("#form_adduser").append('<input id="user_mail1" type="text" placeholder="User email #1"/>').trigger("create");
					counter_add_user_group = 1;
				}
				else {
					alert("An error occured when adding users to the group");
				}
			},
			
			/* answer from the server : error
			or no answer from the server */
			error: function(data)
			{
				console.log("error message :" + JSON.parse(data.responseText).error + "\nerror type : " + data.error_type);
			},
		})
		
		/* answer from the server : error
		or no answer from the server */
		.fail(function()
		{
			alert("An error occured when adding users to the group");
		});
	});
	
	/* * * end of register new user * * */
	
	/* * * * * * * * * * * * *
	*   end of 'group' page  *
	* * * * * * * * * * * * /
	
	
	/* * * * * * * * * * * * * * * 
	*    'creategroup' page      *
	* * * * * * * * * * * * * * */
	
	/* set the 'creategroup' page
	reset the page */
	$("#creategroup").on("pagebeforecreate", function() {
		
		/* clean up the page and related variables */
		$('#loading').empty();
		$("#form_creategroup").empty();
		
		counter_add_user_creategroup = 1;
	});
	
	/* add a user field in the 'groupcreate' page */
	$("#adduser_creategroup").on("click", function() {
		$("#submit_creategroup").show();
		counter_add_user_creategroup += 1;
		$("#form_creategroup").append('<input id="user_mail' + counter_add_user_creategroup + '" placeholder="User mail #' + counter_add_user_creategroup + '" />').trigger("create");
	});
	
	/* register a new group
	queries the secure share servers to register the new group */
	$("#submit_creategroup").on("click", function() {
		
		/* first get members the user wants to add to the group */
		var inputs = document.getElementById("general_form_creategroup").elements;
		var name_groupcreate = inputs[0].value;
		var members_creategroup = "";
		
		if (inputs.length != 1) {
			members_creategroup = '[';
			
			console.log("test");
			for (index = 1; index < inputs.length; ++index) {
				if (index != (inputs.length - 1)) {
					members_creategroup = members_creategroup + ' "' + inputs[index].value + '",';
				}
				else {
					members_creategroup = members_creategroup + ' "' + inputs[index].value + '" ]';
				}
			}
		}
		else {
			members_creategroup = '[]';
		}
		
		
		/* query the server
		register a new group */
		$.ajax({
			/* set the post request to the '/creategrp' url */
			type: "POST",
			url: url_server + "test/creategrp",
			data: '{ "token" : "' + token + '", ' + '"group_name" : "' + name_groupcreate + '" , "members" : ' + members_creategroup + '}',
			dataType: "json",
			
			/* display loading image */
			beforeSend: function(){
				$("#loading").append('<img alt="loading..." src="./img/loading.gif" width="200px"/><br/><span>Please wait while contacting the server...</span>').trigger("create");
			},
			complete: function(){
				$("#loading").empty();
			},
			
			/* wait for some time before deciding the server is down */
			timeout: max_timeout,
			
			/* answer from the server : success */
			success: function(data)
			{
				if (data.group_id =! null){
					alert("The group has been correctly created");
				}
				else {
					alert("An error occured when creating the group");
				}
				
				// clean up
				$("#form_creategroup").empty();
				$("#name_groupcreate").val("");
				counter_add_user_creategroup = 1;
				
			},
			
			/* answer from the server : error
		or no answer from the server */
			error: function(data)
			{
				console.log("error message : " + JSON.parse(data.responseText).error + "\nerror type : " + data.error_type);
			},
		})
		
		/* answer from the server : error
		or no answer from the server */
		.fail(function()
		{
			alert("An error occured when adding users to the group");
		});
	});
	
	/* * * * * * * * * * * * * * *
	* end of 'creategroup' page  *
	* * * * * * * * * * * * * * */
});

/*function refreshtoken (){
	var bouton = document.getElementById("boutongoogle");
	bouton.click();
	
};*/
