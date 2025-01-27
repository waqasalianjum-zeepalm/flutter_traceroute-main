import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_traceroute/flutter_traceroute.dart';
import 'dart:developer';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _tracerouteResults = '';
  final eventChannel = EventChannel("flutter_traceroute_events");

  final _formKey = GlobalKey<FormState>();

  TextEditingController nameController = TextEditingController();

  final _flutterTraceroutePlugin = FlutterTraceroute();

  @override
  void initState() {
    super.initState();
    initPlatformState();

    eventChannel.receiveBroadcastStream().listen((event) {
      print("traceroute_progress=" + event.toString());
      setState(() {
        _tracerouteResults = event.toString() + "\n" + _tracerouteResults;
      });
      // Last line has just {}
      if (event.toString().length <= 2) {
        setState(() {
          _tracerouteResults = "finished!\n" + _tracerouteResults;
        });
      }
    });

  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {

    if (!mounted) return;

  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Qualoo TraceRoute Plug-in sample app'),
        ),
        body: Form(
          key: _formKey,
          child: SingleChildScrollView(
              child: Column(children: <Widget>[
                Padding(
                  padding: EdgeInsets.all(20.0),
                  child: TextFormField(
                    controller: nameController,
                    decoration: InputDecoration(
                      labelText: "Enter Host name",
                      enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(10.0),
                      ),
                    ),
                    // The validator receives the text that the user has entered.
                    validator: (value) {
                      if (value != null && value.isEmpty) {
                        return 'Enter Host Name';
                      }
                      return null;
                    },
                  ),
                ),
                ElevatedButton(
                  onPressed: () async {
                    //if (_formKey.currentState.validate()) {}
                    setState(() {
                      _tracerouteResults = "";
                    });

                    try {
                      var count = 10;
                      var useIcmp = true;
                      await _flutterTraceroutePlugin.traceroute(nameController.text, count, useIcmp);

                    } on PlatformException catch (err) {
                      log("error running traceroute " + err.toString());
                    }
                  },
                  child: Text('Go'),
                ),
                Center(
                  child: Text('$_tracerouteResults\n'), //
                )
              ]))),
              ),
    );
  }
}
